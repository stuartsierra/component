(defproject com.stuartsierra/component "0.2.4-SNAPSHOT"
  :description "Managed lifecycle of stateful objects"
  :url "https://github.com/stuartsierra/component"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :min-lein-version "2.1.3"  ; added :global-vars
  :dependencies [[com.stuartsierra/dependency "0.1.1"]]
  :global-vars {*warn-on-reflection* true}
  :aliases {"test-all"
            ["with-profile" "clj1.4:clj1.5:clj1.6:clj1.7" "test"]}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["dev"]}
             :clj1.7 {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]
                      :repositories {"sonatype-oss-public"
                                     "https://oss.sonatype.org/content/groups/public"}}
             :clj1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :clj1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :clj1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}})
