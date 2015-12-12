(ns bltool.data.xboxlive
  (:require [bltool.data.default :refer [read-games]])
  (:require [bltool.flags :refer :all])
  (:require [clj-http.client :as http])
  (:require [slingshot.slingshot :refer [throw+]])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :refer [trim, lower-case]]))

(register-flags ["--xbox-name" "Xbox Live GamerTag"]
  ["--xbox-360-platform" "Platform string to use for Xbox 360 games (recommended: 360, Xbox, XBLA, XNA, XbxGoD)" :default "360"]
  ["--xbox-one-platform" "Platform string to use for Xbox One games (recommended: XBO, Xbox, XbxGoD)" :default "XBO"])

(def xboxapi-key "527f7671354689326b1856b986c564192cd05df6")

(defn- xml-to-map
  [tag]
    (let [tag-content (fn [tag] [(:tag tag) (apply str (:content tag))])]
      (into {} (map tag-content (:content tag)))))

(defn- check-games
  [games]
  (if (empty? games)
    (throw+ (str "Couldn't find any games for Xbox Live GamerTag" (:xbox-name *opts*)
                 " -- are you sure you specified the right --xbox-name?"))
    games))

(defn- extract-game-data 
  [map]
  (hash-map :name (trim (:name map)) :played (if (:hoursOnRecord map) "unfinished" "unplayed")))

(defn- remove-the
  [name]
  (if (.startsWith name "the ") (subs name 4) name))

(defn- xboxapi-request
  [url]
  (let [reply (http/get url { :headers {"X-AUTH" xboxapi-key} :throw-exceptions false :accept :json })]
    (if (= (get (reply :headers) "x-ratelimit-remaining") 0) (throw+ "Xboxapi.com free access overloaded, try again in an hour or so.") reply)))


; All games in XBL are by definition played since they are not registered until started on the console
(defn- extract-xbox-game
  [platform maxname json]
  { :id "0" :name (get json "name") :platform platform :progress (if (= (get json "currentGamerscore") (get json maxname)) "completed" "unfinished")})

(defn- get-xbox-games
  [url platform maxname]
  (let [strreply (xboxapi-request url) 
        json (json/read-str (:body strreply))]
        (map #(extract-xbox-game platform maxname %) (get json "titles"))))


(defmethod read-games "xboxlive" [_ source]
  (if (not (:xbox-name *opts*))
    (throw+ "No Xbox Live gamertag specified - use the --xbox-name option."))
  (let [name (:xbox-name *opts*)
        getxurl (str "https://xboxapi.com/v2/xuid/" name)
        xuidreply (xboxapi-request getxurl)]
        (if (= (xuidreply :status) 404) (throw+ (str "Couldn't get Xbox ID for gamertag" name ", check it is correct and xboxapi.com is available."))
            (let [xuid (xuidreply :body)]
              (println "Got Xuid" xuid "for gamertag" name ".")
              (let [get360url (str "https://xboxapi.com/v2/" xuid "/xbox360games")
                games360 (get-xbox-games get360url (:xbox-360-platform *opts*) "totalGamerscore")
                getOneUrl (str "https://xboxapi.com/v2/" xuid "/xboxonegames")
                gamesOne (get-xbox-games getOneUrl (:xbox-one-platform *opts*) "maxGamerscore")
                allGames (concat games360 gamesOne)]
                (println "Found" (count games360) "Xbox 360 and" (count gamesOne) "Xbox One games.")
                (check-games (sort-by (comp remove-the lower-case :name) allGames)))))))

