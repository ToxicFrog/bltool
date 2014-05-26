(ns bltool.data.edn
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clojure.edn :as edn]))

(defmethod read-games "edn" [_ source]
  (edn/read (java.io.PushbackReader. source)))

(defmethod write-games "edn" [_ games sink]
  (.write sink (str (vec games))))
