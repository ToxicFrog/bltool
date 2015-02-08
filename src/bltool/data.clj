(ns bltool.data
  (:require [bltool.data.default :as default])
  (:require [bltool.data steam backloggery edn text xboxlive])
  (:require [bltool.flags :refer :all]))

(def read-games default/read-games)
(def write-games default/write-games)
