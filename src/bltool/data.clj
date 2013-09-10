(ns bltool.data
  (:require [bltool.flags :refer :all])
  (:require [bltool.data.default :as default]
            (bltool.data steam
                         backloggery
                         edn
                         text
                         html)))

(def read-games default/read-games)
(def write-games default/write-games)
