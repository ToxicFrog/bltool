(ns backloggery.flags
  (:require [clojure.tools.cli :as cli]))

(def flags [])

(def ^:dynamic *opts* {})

(defn register-flags [& new-flags]
  (def flags (into flags new-flags)))

(defn getopts [args] (apply cli/cli args flags))
