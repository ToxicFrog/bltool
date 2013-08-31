(ns backloggery.from.steam
  (:require [clj-http.client :as http])
  (:require [clojure.data.xml :as xml])
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data.default :refer [read-games]]))

(register-flags ["--steam-name" "Steam Community name"])

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))  

(defmethod read-games "steam" [_]
  (let [name (:steam-name *opts*)
        url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp :name xml-to-map))
      sort
      (map (fn [name] { :name name :platform "Steam" :progress "unplayed" })))))
