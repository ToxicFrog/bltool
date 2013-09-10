(ns bltool.data.edn
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clojure.edn :as edn]))

(defmethod read-games "edn" [_]
  (edn/read (java.io.PushbackReader. (:input *opts*))))
  
(defmethod write-games "edn" [_ games]
  (.write (:output *opts*) (str (vec games))))             
