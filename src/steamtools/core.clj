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

(defn- bl-username [cookies] ((cookies "c_user") :value))

(defn- bl-games
  "Retrieve your Backloggery game list."
  [cookies]
  (let [username (bl-username cookies)
        response (http/get "http://backloggery.com/games.php" 
                           {:cookies cookies
                            :query-params {"user" username}})]
    (->> response
         :body .getBytes html/parse
         (tag-seq :footer) first
         (tag-seq :script) first
         :content)))

(defn -main
  [& args]
  (->> args (apply bl-login) (bl-games) println))
