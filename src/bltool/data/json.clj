(ns bltool.data.json
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clojure.data.json :as json])
  (:require [clojure.pprint :as pprint]))

(defmethod read-games "json" [_ source]
  (json/read (java.io.PushbackReader. source) :key-fn keyword))

(defmethod write-games "json" [_ games sink]
  (binding [*out* sink]
    (json/pprint (vec games)))
  (.write sink "\n"))
