(ns backloggery.data.text
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data.default :refer :all])
  (:require [clojure.string :refer [split join]]))

(defn- to-text [game]
  (format "%-8s  %-10s  %s" (:platform game) (:progress game) (:name game)))

(defn- from-text [game]
  (let [[platform progress name] (split game #"\s+" 3)]
    {:name name :platform platform :progress progress}))

(defmethod read-games "text" [_]
  (->> (:input *opts*)
       line-seq
       (map from-text)))

(defmethod write-games "text" [_ games]
  (->> games
       (map to-text)
       (join "\n")
       (.write (:output *opts*))))
