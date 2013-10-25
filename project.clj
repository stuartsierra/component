(defproject com.stuartsierra/component "0.1.0-SNAPSHOT"
  :description "Managed lifecycle of stateful objects"
  :url "https://github.com/stuartsierra/component"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/dependency "0.1.1"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}
             :clj1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}})
