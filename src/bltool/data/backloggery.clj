(ns bltool.data.backloggery
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clj-http.client :as http])
  (:require [clj-http.util :refer [url-decode]])
  (:require [clojure.string :refer [split]])
  (:require [slingshot.slingshot :refer [throw+ try+]])
  (:require [crouton.html :as html]))

(register-flags ["--bl-name" "backloggery username"]
                ["--bl-pass" "backloggery password"]
                ["--bl-stealth" "use 'stealth add' and 'stealth edit' when updating backloggery"
                 :flag true :default true]
                ["--bl-region" "backloggery region code to use when updating backloggery" :default "0"])

(def ^:dynamic *user* nil)
(def ^:dynamic *cookies* nil)

(defn- tag-seq [tag body]
  (filter #(= tag (:tag %)) (xml-seq body)))

(defn- bl-result [response]
  (let [divs (tag-seq :div response)
        update-g (filter #(= "update-g" (:class (:attrs %))) divs)
        update-r (filter #(= "update-r" (:class (:attrs %))) divs)]
    (cond
      (first update-g) (->> update-g first :content (apply str))
      (first update-r) (->> update-r first :content (apply str))
      :default "unknown result")))


;; Logging in to Backloggery

(defn- bl-login
  "Make a login request to Backloggery, and return the authentication cookies."
  [name pass]
  (println "Logging into Backloggery as" name)
  (let [response (http/post
                   "http://backloggery.com/login.php"
                   {:multipart [{:name "username" :content name}
                                {:name "password" :content pass}
                                {:name "duration" :content "hour"}]})]
    ; BL login request returns 200 OK if the login *fails*, and 302 FOUND otherwise.
    (if (= 302 (:status response))
      (:cookies response)
      (throw+ "Unable to log in to Backloggery. Please check your username and password."))))


;; Reading the game list

(defn- bl-more-games
  [cookies user params]
  (let [defaults {"user" user
                  "console" "" "rating" "" "status" "" "unplayed" "" "own" "" "search" ""
                  "comments" "" "region" "" "region_u" "0" "wish" "" "alpha" ""}
        params (conj defaults params)
        response (http/get "http://backloggery.com/ajax_moregames.php"
                           {:cookies cookies
                            :query-params params
                            :headers {"referer" (str "http://backloggery.com/games.php?user=" user)}})]
    (-> response :body java.io.StringReader. html/parse)))

(defn- bl-get-compilation
  [cookies user params]
  (let [defaults {"user" user
                  "total" "50"
                  "region_u" "0"
                  "console" "" "rating" "" "status" "" "unplayed" "" "own" "" "search" ""
                  "comments" "" "region" "" "wish" "" "alpha" ""}
        params (conj defaults params)
        response (http/get "http://backloggery.com/ajax_expandcomp.php"
                           {:cookies cookies
                            :query-params params
                            :headers {"referer" (str "http://backloggery.com/games.php?user=" user)}})]
    (-> response :body java.io.StringReader. html/parse)))

(defn- bl-extract-params [body]
  (some->> body (tag-seq :input) first :attrs :onclick
           (re-find #"getMoreGames\(([^,]+), *'([^']+)', *'([^']+)', *([^)]+)\)")
           rest
           (map url-decode)
           (zipmap ["aid" "temp_sys" "ajid" "total"])))

(defn- bl-extract-params-for-compilation [body]
  (some->> body (tag-seq :span) first :attrs :onclick
           (re-find #"getComp\(([^,]+), *'([^']+)', *'([^)]+)'\)")
           rest
           (map url-decode)
           (zipmap ["aid", "comp_sys", "comp"])))

; {:tag :a, :attrs {:href update.php?user=toxicfrog&gameid=4160408}
(defn- gamebox-to-game [gamebox]
  (let [progress-map { "(M)" "mastered" "(C)" "completed" "(B)" "beaten" "(U)" "unfinished" "(u)" "unplayed" "(-)" "null" }
        bold (->> gamebox (tag-seq :b) (map :content) (map #(apply str %)) (map clojure.string/trim))
        id (-> (tag-seq :a gamebox) first :attrs :href (split #"gameid=") last)
        name (first bold)
        platform (second bold)
        progress (->> gamebox (tag-seq :img) second :attrs :alt progress-map)]
    { :id id :name name :platform platform :progress progress }))

; extract params from collection box
; then invoke bl-get-compilation
(declare bl-extract-games) ; mutual recursion
(defn- expand-collection [section]
  (->> section
       bl-extract-params-for-compilation
       (bl-get-compilation *cookies* *user*)
       bl-extract-games))

; collections can be identified by the lack of <a> tags
; and also by the first <img> tag having src="images/compilation.gif"
(defn- games-from-section
  [section]
  (cond
    (= "images/compilation.gif" (some->> section (tag-seq :img) first :attrs :src)) (expand-collection section)
    (->> section (tag-seq :a) first) [(gamebox-to-game section)]
    :else nil))

(defn- bl-extract-games [body]
  (->> body (tag-seq :section)
       (filter #(.contains (:class (:attrs %)) "gamebox"))
       (mapcat games-from-section)))

(defn- read-all-games [extra-params]
  (loop [games []
         params { "aid" "1" "temp_sys" "ZZZ" "ajid" "0" "total" "0" }]
    (println "Fetched" (count games) "games from Backloggery...")
    (if params
      (let [page (bl-more-games *cookies* *user* (conj extra-params params))]
        (recur (concat games (bl-extract-games page)) (bl-extract-params page)))
      (sort-by :name games))))

(defmethod read-games "backloggery" [_ source]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (binding [*user* user
              *cookies* cookies]
      (read-all-games {}))))

(defmethod read-games "bl-html-debug" [_ source]
  (->> source
       slurp
       java.io.StringReader.
       html/parse
       bl-extract-games
       (sort-by :name)))

; backloggery wishlist
(defmethod read-games "bl-wishlist" [_ source]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (binding [*user* user
              *cookies* cookies]
      (read-all-games {"wish" "1"}))))

(defmethod write-games "bl-wishlist" [_ games sink]
  (println "No support for adding wishlist games yet."))

;; Adding new games

(defn- complete-code [desc]
  ({"unplayed" "1"
    "unfinished" "1"
    "beaten" "2"
    "completed" "3"
    "mastered" "4"
    "null" "5"}
   desc))

(defn- add-game [cookies game]
  (let [user (:bl-name *opts*)
        defaults {"comp" "" "orig_console" "" "region" "0" "own" "1"
                  "achieve1" "" "achieve2" "" "online" "" "note" ""
                  "rating" "8" "submit2" "Stealth Add" "wishlist" "0"}
        params (conj defaults
                     {"name" (:name game)
                      "console" (:platform game)
                      "complete" (complete-code (:progress game))
                      "unplayed" (if (= "unplayed" (:progress game)) "1" "0")
                      "region" (:bl-region *opts*)}
                     (if (:bl-stealth *opts*)
                       {"submit2" "Stealth Add"}
                       {"submit1" "Add Game"}))
        body (map (fn [[k v]] {:name k :content v}) params)]
    (printf "Adding %s game '%s' to backloggery:" (:progress game) (:name game))
    (let [response (http/post
                     "http://backloggery.com/newgame.php"
                     {:cookies cookies
                      ;:debug true :debug-body true
                      :query-params {"user" user}
                      :multipart (vec body)})]
      (->> response
           :body
           java.io.StringReader.
           html/parse
           bl-result
           (printf " %s\n")))))

(defn- safe-add-game [cookies game]
  (try+
    (add-game cookies game)
    (catch Exception _
      (printf "\nError adding game '%s' -- check your input data.\n" (str (:name game)))
      (println "Error was: " (:message &throw-context)))))

(defmethod write-games "backloggery" [_ games sink] (write-games "bl-add" games sink))

(defmethod write-games "bl-add" [_ games sink]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (dorun (map #(safe-add-game cookies %) games))))


;; Deleting games

(defn- delete-game [cookies game]
  (let [user (:bl-name *opts*)
        params {"user" user "delete2" "Stealth Delete"}
        body (map (fn [[k v]] {:name k :content v}) params)]
    (printf "Deleting '%s' from backloggery:" (:name game))
    (let [response (http/post "http://backloggery.com/update.php"
                              {:cookies cookies
                               ;:debug true :debug-body true
                               :query-params {"user" user "gameid" (:id game)}
                               :multipart (vec body)})]
      (->> response
           :body
           java.io.StringReader.
           html/parse
           bl-result
           (printf " %s\n")))))

(defmethod write-games "bl-delete" [_ games sink]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (dorun (map #(delete-game cookies %) games))))


;; Editing games

; we need the following fields for edit
; name console complete - filled in from game list
; own note wishlist playing - *can be* filled in from game list, but currently not
; comp orig_console region achieve1 achieve2 online rating comments - must be filled in from details page

; function bl:details(game)
;     assert(type(game) == "number", 'invalid argument to bl:details')

;     game = assert(self:games()[game], 'no game with id '..game)

;     if game._details then return game end

;     local body = assert(request(self, { user = self.user, gameid = game.id }, "GET", "http://backloggery.com/update.php"))

;     local function set(key, value)
;         if not game[key] then
;             game[key] = value
;         end
;     end

;     -- name, console, complete, note, wishlist, and playing were already
;     -- filled in by the initial loading of the game list
;     -- FIXME own should be as well

;     -- this leaves: comp, orig_console, region
;     -- achieve1, achieve2, online
;     -- rating, comments

;     set("comp", body:Find("input", "name", "comp").value)

;     set("orig_console", body:Find("select", "name", "orig_console"):Find("option", "selected", true).value)
;     set("_orig_console_str", bl.platforms[game.orig_console])

;     set("region", tonumber(body:Find("select", "name", "region"):Find("option", "selected", true).value))
;     set("_region_str", bl.regions[game.region])

;     set("achieve1", tonumber(body:Find("input", "name", "achieve1").value) or "")
;     set("achieve2", tonumber(body:Find("input", "name", "achieve2").value) or "")

;     set("online", body:Find("input", "name", "online").value)
;     set("comments", body:Find("textarea", "name", "comments"):Content())

;     -- set the "details" flag on this game, recording that all fields are filled in
;     set("_details", true)

;     return game
; end

; -- upload the changes we've made to a game structure to the server
; function bl:editgame(game)
;     assert(type(game) == "number", 'invalid argument to bl:editgame')

;     game = assert(self:games()[game], 'no game with id '..game)

;     -- fill in any missing fields
;     self:details(game.id)

;     -- the _ derived fields override the original ones
;     game.console = bl.rplatforms[game._console_str]
;     game.orig_console = bl.rplatforms[game._orig_console_str]
;     game.region = bl.rregions[game._region_str] or 0
;     game.rating = game._stars - 1; if game.rating < 0 then game.rating = 8 end
;     game.complete = bl.completecode(game._complete_str)
;     game.submit2 = "Stealth Save"

;     -- create request
;     local r,e = request(self, game, "POST", "http://backloggery.com/update.php?user="..self.user.."&gameid="..game.id)

;     -- update internal structures
;     self:games()[game.id] = game

;     return game
; end

(defmethod read-games "backloggery-edit" [_ source]
  (println "No support for editing games yet."))

(defmethod write-games "backloggery-edit" [_ games sink]
  (println "No support for editing games yet."))
