(ns backloggery.from.backloggery
  (:require [clj-http.client :as http])
  (:require [crouton.html :as html])
  (:require [backloggery.flags :refer :all])
  (:require [clojure.string :refer [split]])
  (:require [backloggery.data.default :refer :all]))

(register-flags ["--bl-name" "backloggery username"]
                ["--bl-pass" "backloggery password"]
                ["--bl-stealth" "use 'stealth add' and 'stealth edit' when updating backloggery" :flag true])

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
      nil)))


;; Reading the game list

(defn- bl-more-games
  [cookies user params]
  (let [params (conj params
                     {"user" user
                      "console" "" "rating" "" "status" "" "unplayed" "" "own" "" "search" ""
                      "comments" "" "region" "" "region_u" "0" "wish" "" "alpha" ""})
        response (http/get "http://backloggery.com/ajax_moregames.php"
                           {:cookies cookies
                            :query-params params
                            :headers {"referer" (str "http://backloggery.com/games.php?user=" user)}})]
    (-> response :body java.io.StringReader. html/parse)))

(defn- bl-extract-params [body]
  (some->> body (tag-seq :input) first :attrs :onclick
           (re-find #"getMoreGames\(([^,]+), *'([^']+)', *'([^']+)', *([^)]+)\)")
           rest
           (zipmap ["aid" "temp_sys" "ajid" "total"])))

; {:tag :a, :attrs {:href update.php?user=toxicfrog&gameid=4160408}
(defn- gamebox-to-game [gamebox]
  (let [progress-map { "(M)" "mastered" "(C)" "complete" "(B)" "beaten" "(U)" "unfinished" "(u)" "unplayed" "(-)" "null" }
        bold (->> gamebox (tag-seq :b) (map :content) (map #(apply str %)) (map clojure.string/trim))
        id (-> (tag-seq :a gamebox) first :attrs :href (split #"gameid=") last)
        name (first bold)
        platform (second bold)
        progress (->> gamebox (tag-seq :img) second :attrs :alt progress-map)]
    { :id id :name name :platform platform :progress progress }))

(defn- bl-extract-games [body]
  (->> body (tag-seq :section)
       (filter #(= "gamebox" (:class (:attrs %))))
       (map gamebox-to-game)
       ; filter out any collections; these will have an HTML tag as the name rather than a string
       (filter :platform)))

(defmethod read-games "backloggery" [_]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (loop [games []
           params { "aid" "1" "temp_sys" "ZZZ" "ajid" "0" "total" "0" }]
      (println "Fetched" (count games) "games from Backloggery...")
      (if (and params (= 0 (count games)))
        (let [page (bl-more-games cookies user params)]
          (recur (concat games (bl-extract-games page)) (bl-extract-params page)))
        (sort-by :name games)))))


;; Adding new games

(defn- complete-code [desc]
  ({"unplayed" "1"
    "unfinished" "1"
    "beaten" "2"
    "complete" "3"
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
                      "unplayed" (if (= "unplayed" (:progress game)) "1" "0")}
                     (if (:bl-stealth *opts*)
                       {"submit2" "Stealth Add"}
                       {"submit1" "Add Game"}))
        body (map (fn [[k v]] {:name k :content v}) params)]
    (printf "Adding %s game '%s' to backloggery:" (:progress game) (:name game))
    (let [response (http/post "http://backloggery.com/newgame.php"
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

(defmethod write-games "backloggery" [_ games] (write-games "bl-add" games))

(defmethod write-games "bl-add" [_ games]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (dorun (map #(add-game cookies %) games))))


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

(defmethod write-games "bl-delete" [_ games]
  (let [user (:bl-name *opts*)
        pass (:bl-pass *opts*)
        cookies (bl-login user pass)]
    (dorun (map #(delete-game cookies %) games))))


;; Editing games

(defmethod write-games "backloggery-edit" [_ games]
  (println "No support for editing games yet."))
