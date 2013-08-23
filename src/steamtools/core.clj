(ns steamtools.core
  (:require [clj-http.client :as http])
  (:require [clojure.tools.cli :as cli])
  (:require [clojure.data.xml :as xml])
  (:require [crouton.html :as html])
  (:gen-class))

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))  

(defn- tag-seq [tag body]
  (filter #(= tag (:tag %)) (xml-seq body)))

(defn- steam-games
  "Download the game list for a user from SteamCommunity."
  [name]
  (let [url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp :name xml-to-map))
      sort)))

(defn- bl-login
  "Make a login request to Backloggery, and return the authentication cookies."
  [name pass]
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

(defn- bl-games
  "Retrieve your Backloggery game list."
  [user pass]
  (let [cookies (bl-login user pass)]
    (loop [games []
           params { "aid" "1" "temp_sys" "ZZZ" "ajid" "0" "total" "0" }]
      (println (count games) params)
      (if params
        (let [page (bl-more-games cookies user params)]
          (recur (conj games page) (bl-extract-params page)))
        games))))

(defn -main
  [& args]
  (println (apply bl-games args)))

