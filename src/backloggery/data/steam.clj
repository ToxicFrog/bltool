(ns backloggery.data.steam
  (:require [clj-http.client :as http])
  (:require [clojure.data.xml :as xml])
  (:require [clojure.string :refer [trim]])
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data.default :refer [read-games]]))

(register-flags ["--steam-name" "Steam Community name"]
                ["--steam-platform" "Default platform to use for Steam games (recommended: PC, PCDL, or Steam)" :default "PC"])

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))  

(defmethod read-games "steam" [_]
  (let [name (:steam-name *opts*)
        url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp trim :name xml-to-map))
      sort
      (map (fn [name] { :name name :platform (:steam-platform *opts*) :progress "unplayed" })))))
