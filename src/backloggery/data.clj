(ns backloggery.data
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data.default :as default]
            (backloggery.data steam
                              backloggery
                              edn
                              text
                              html)))

(def read-games default/read-games)
(def write-games default/write-games)
