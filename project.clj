(defproject bltool "0.1.2"
  :description "Simple tools for managing a Backloggery game collection"
  :url "https://github.com/toxicfrog/backloggery"
  :license {:name "Apache License v2"
            :url "http://opensource.org/licenses/Apache-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.typed "0.2.5"]
                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/tools.cli "0.2.4"]
                 [clj-http "0.7.6"]
                 [slingshot "0.10.3"]
                 [crouton "0.1.1"]]
  :core.typed {:check [bltool.core
                       bltool.flags
                       bltool.data
                       bltool.data.default
                       bltool.data.steam
                       bltool.data.backloggery
                       bltool.data.edn
                       bltool.data.text]}
  :profiles {:uberjar {:aot :all}}
  :main bltool.core)
