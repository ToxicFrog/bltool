(defproject bltool "0.1.0-SNAPSHOT"
  :description "Simple tools for managing a Steam game collection"
  :url "https://github.com/toxicfrog/backloggery"
  :license {:name "Apache License v2"
            :url "http://opensource.org/licenses/Apache-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.6"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/data.xml "0.0.7"]
                 [slingshot "0.10.3"]
                 [crouton "0.1.1"]]
  :main bltool.core)
