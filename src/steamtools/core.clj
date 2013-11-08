(ns steamtools.core
  (:require [clj-http.client :as http])
  (:require [clojure.tools.cli :as cli])
  (:require [clojure.data.xml :as xml])
  (:gen-class))

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))  

(defn- get-games
  "Download the game list for a user from SteamCommunity."
  [name]
  (let [url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp :name xml-to-map))
      sort)))

(defn -main
  [& args]
  (->> (first args) get-games (map println) doall))
