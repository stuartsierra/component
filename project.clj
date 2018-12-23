(defproject com.stuartsierra/component "0.3.3-SNAPSHOT"
  :description "Managed lifecycle of stateful objects"
  :url "https://github.com/stuartsierra/component"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/dependency "0.2.0"]
                 [org.clojure/clojure "1.7.0"]]
  :aliases {"test-all" ["with-profile" "+1.7:+1.8:+1.9:+1.10" "test"]}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha1"]]
                   :source-paths ["dev"]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}})
