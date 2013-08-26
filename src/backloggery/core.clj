(ns backloggery.core
  (:require [clj-http.client :as http])
  (:require [clojure.tools.cli :as cli])
  (:require [clojure.data.xml :as xml])
  (:require [crouton.html :as html])
  (:gen-class))

; GET /ajax_moregames.php? temp_sys=ZZZ &total=0 &own= &wish= &search= &region_u=0 &user=ToxicFrog
; &unplayed= &alpha= &region= &console= &status= &rating= &aj_id=0 &id=1 comments= HTTP/1.1

(defn getopts [args]
  (cli/cli args
       ["--steam-name" "steam community username"]
       ["--bl-name" "backloggery username"]
       ["--bl-pass" "backloggery password"]))

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))  

(defn- steam-games
  "Download the game list for a user from SteamCommunity."
  [name]
  (let [url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp :name xml-to-map))
      sort
      (map (fn [name] { :name name :platform "Steam" :progress "unplayed" })))))

(defn- tag-seq [tag body]
  (filter #(= tag (:tag %)) (xml-seq body)))

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

(defn bl-extract-params [body]
  (some->> body (tag-seq :input) first :attrs :onclick
           (re-find #"getMoreGames\(([^,]+), *'([^']+)', *'([^']+)', *([^)]+)\)")
           rest
           (zipmap ["aid" "temp_sys" "ajid" "total"])))

(defn gamebox-to-game [gamebox]
  (let [progress-map { "(M)" "mastered" "(C)" "complete" "(B)" "beaten" "(U)" "unfinished" "(u)" "unplayed" "(-)" "null" }
        bold (->> gamebox (tag-seq :b) (map :content) (map #(apply str %)) (map clojure.string/trim))
        name (first bold)
        platform (second bold)
        progress (->> gamebox (tag-seq :img) second :attrs :alt progress-map)]
    { :name name :platform platform :progress progress }))

(defn bl-extract-games [body]
  (->> body (tag-seq :section)
       (filter #(= "gamebox" (:class (:attrs %))))
       (map gamebox-to-game)
       ; filter out any collections; these will have an HTML tag as the name rather than a string
       (filter :platform)))

(defn- bl-games
  "Retrieve your Backloggery game list."
  [user pass]
  (let [cookies (bl-login user pass)]
    (loop [games []
           params { "aid" "1" "temp_sys" "ZZZ" "ajid" "0" "total" "0" }]
      (println "Fetched" (count games) "games from Backloggery...")
      (if params
        (let [page (bl-more-games cookies user params)]
          (recur (concat games (bl-extract-games page)) (bl-extract-params page)))
        (sort-by :name games)))))

(defn -main
  [& args]
  (let [[opts args help] (getopts args)
        steam-game-list (steam-games (:steam-name opts))
        bl-game-list (bl-games (:bl-name opts) (:bl-pass opts))]
    (println "==== BACKLOGGERY ====")
    (dorun (map println bl-game-list))
    (println "======  STEAM  ======")
    (dorun (map println steam-game-list))))
