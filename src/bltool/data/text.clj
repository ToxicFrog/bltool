(ns bltool.data.text
  (:require [bltool.data.default :refer :all])
  (:require [bltool.flags :refer :all])
  (:require [clojure.string :refer [split join trim]]))

(defn- to-text [game]
  (format "%8s  %-10s  %-10s  %s" (:id game) (:platform game) (:progress game) (:name game)))

(defn- from-text [game]
  (let [[id platform progress name] (-> game trim (split #"\s+" 4))]
    {:id id :name name :platform platform :progress progress}))

(defmethod read-games "text" [_ source]
  (->> source
       line-seq
       (map from-text)))

(defmethod write-games "text" [_ games sink]
  (->> games
       (map to-text)
       (join "\n")
       (.write sink))
  (.write sink "\n"))
