(ns bltool.data.edn
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clojure.edn :as edn])
  (:require [clojure.pprint :as pprint]))

(defmethod read-games "edn" [_ source]
  (edn/read (java.io.PushbackReader. source)))

(defmethod write-games "edn" [_ games sink]
  (pprint/pprint (vec games) sink)
  (.write sink "\n"))
