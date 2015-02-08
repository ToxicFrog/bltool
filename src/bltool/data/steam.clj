(ns bltool.data.steam
  (:require [bltool.data.default :refer [read-games]])
  (:require [bltool.flags :refer :all])
  (:require [clj-http.client :as http])
  (:require [clojure.data.xml :as xml])
  (:require [slingshot.slingshot :refer [throw+]])
  (:require [clojure.string :refer [trim, lower-case]]))

(register-flags ["--steam-name" "Steam Community name"]
                ["--steam-platform" "Default platform to use for Steam games (recommended: PC, PCDL, or Steam)" :default "PC"])

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))

(defn- check-games
  [games]
  (if (empty? games)
    (throw+ (str "Couldn't find any games at http://steamcommunity.com/id/" (:steam-name *opts*)
                 " -- are you sure you specified the right --steam-name?"))
    games))

(defn- extract-game-data 
  [map]
  (hash-map :name (trim (:name map)) :played (if (:hoursOnRecord map) "unfinished" "unplayed")))

(defn- remove-the
  [name]
  (if (.startsWith name "the ") (subs name 4) name))

(defmethod read-games "steam" [_ source]
  (if (not (:steam-name *opts*))
    (throw+ "No Steam Community name specified - use the --steam-name option."))
  (let [name (:steam-name *opts*)
        url (str "http://steamcommunity.com/id/" name "/games?tab=all&xml=1")]
    (->> url http/get :body xml/parse-str xml-seq (filter #(= :game (:tag %)))
      (map (comp extract-game-data xml-to-map))
      (sort-by (comp remove-the lower-case :name))
      (map (fn [xdata] { :id "0" :name (xdata :name) :platform (:steam-platform *opts*) :progress (xdata :played) }))
      check-games)))
