(defproject com.stuartsierra/component "0.3.1-SNAPSHOT"
  :description "Managed lifecycle of stateful objects"
  :url "https://github.com/stuartsierra/component"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/dependency "0.2.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["dev"]}})
