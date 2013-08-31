(ns backloggery.data.default
  (:require [backloggery.flags :refer :all])
  (:require [slingshot.slingshot :refer [throw+]]))

(register-flags ["--from"
                 "What type of data source to read the games/edits from."]
                ["--to"
                 "What type of destination to write the changes to."])

(defmulti read-games (fn [format] format))
(defmulti write-games (fn [format games] format))

(defmethod read-games :default [format]
  (throw+ (str "No support for reading from '" format "'. Use '--help formats' to list available formats.")))

(defmethod read-games nil [format]
  (throw+ "No data source specified. Use '--from <format>' to specify a source format."))

(defmethod write-games :default [format games]
  (throw+ (str "No support for writing to '" format "'. Use '--help formats' to list available formats.")))

(defmethod write-games nil [format games]
  (throw+ "No data destination specified. Use '--to <format>' to specify a destination format."))
