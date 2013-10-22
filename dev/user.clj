(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :refer :all]))

(defrecord DummyComponent [name deps]
  Lifecycle
  (start [this] (prn 'start name) (assoc this :started true))
  (stop [this] (prn 'stop name) (assoc this :started false)))

(defn dummy-component [name & deps]
  (using (->DummyComponent name deps)
    (vec deps)))

(def database (dummy-component :database))
(def message-broker (dummy-component :message-broker))
(def scheduler (dummy-component :scheduler :database))
(def background-job (dummy-component :background-job :scheduler :database :message-broker))
(def application (dummy-component :application :database :message-broker))
(def web-server (dummy-component :web-server :application))

(defrecord MySystem []
  Lifecycle
  (start [this]
    (start-system this (keys this)))
  (stop [this]
    (stop-system this (keys this))))

(defn my-system []
  (map->MySystem {:database database
                  :message-broker message-broker
                  :scheduler scheduler
                  :background-job background-job
                  :application application
                  :web-server web-server}))
