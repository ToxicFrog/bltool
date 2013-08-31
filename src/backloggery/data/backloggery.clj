(ns backloggery.from.backloggery
  (:require [clj-http.client :as http])
  (:require [crouton.html :as html])
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data.default :refer :all]))

(register-flags ["--bl-name" "backloggery username"]
                ["--bl-pass" "backloggery password"])

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

(defn- bl-extract-params [body]
  (some->> body (tag-seq :input) first :attrs :onclick
           (re-find #"getMoreGames\(([^,]+), *'([^']+)', *'([^']+)', *([^)]+)\)")
           rest
           (zipmap ["aid" "temp_sys" "ajid" "total"])))

(defn- gamebox-to-game [gamebox]
  (let [progress-map { "(M)" "mastered" "(C)" "complete" "(B)" "beaten" "(U)" "unfinished" "(u)" "unplayed" "(-)" "null" }
        bold (->> gamebox (tag-seq :b) (map :content) (map #(apply str %)) (map clojure.string/trim))
        name (first bold)
        platform (second bold)
        progress (->> gamebox (tag-seq :img) second :attrs :alt progress-map)]
    { :name name :platform platform :progress progress }))

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

(defmethod write-games "backloggery" [_]
  (println "not implemented yet"))