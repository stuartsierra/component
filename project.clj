(defproject com.stuartsierra/component "0.4.1-SNAPSHOT"
  :description "Managed lifecycle of stateful objects"
  :url "https://github.com/stuartsierra/component"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/dependency "0.2.0"]
                 [org.clojure/clojure "1.7.0" :scope "provided"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha1"]]
                   :source-paths ["dev"]}})
