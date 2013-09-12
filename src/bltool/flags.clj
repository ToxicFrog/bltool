(ns bltool.flags
  (:require [clojure.core.typed :as t :refer [fn> ann]])
  (:require [clojure.tools.cli :as cli]))

(ann flags (t/Vec Any))
(def flags [])

(ann *opts* (t/Map String String))
(def ^:dynamic *opts* {})

(ann register-flags [* -> Nothing])
(defn register-flags [& new-flags]
  (def flags (into flags new-flags)))

(ann getopts [(t/Vec Any) -> Any])
(defn getopts [args] (apply cli/cli args flags))
