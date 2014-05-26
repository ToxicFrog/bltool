(ns bltool.data.default
  (:require [bltool.flags :refer :all])
  (:require [clojure.core.typed :as t :refer [fn> ann]])
  (:require [slingshot.slingshot :refer [throw+]]))

(register-flags ["--from"
                 "What type of data source to read the games/edits from. Use '--help formats' for a list of formats."]
                ["--to"
                 "What type of destination to write the changes to. Use '--help formats' for a list of formats."])

(t/def-alias BLGame '{:id String :name String :platform String :progress String})
(t/def-alias BLGameList (t/Vec BLGame))

(ann read-games [String -> BLGameList])
(ann write-games [String BLGameList -> Nothing])
(defmulti read-games (fn [format source] format))
(defmulti write-games (fn [format games sink] format))

(defmethod read-games :default [format source]
  (throw+ (str "No support for reading from '" format "'. Use '--help formats' to list available formats.")))

(defmethod read-games nil [format source]
  (throw+ "No data source specified. Use '--from <format>' to specify a source format."))

(defmethod write-games :default [format games sink]
  (throw+ (str "No support for writing to '" format "'. Use '--help formats' to list available formats.")))

(defmethod write-games nil [format games sink]
  (throw+ "No data destination specified. Use '--to <format>' to specify a destination format."))
