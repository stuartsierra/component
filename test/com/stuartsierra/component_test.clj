(ns com.stuartsierra.component-test
  (:require [clojure.test :refer (deftest is are)]
            [clojure.set :refer (map-invert)]
            [com.stuartsierra.component :as component]))

(def ^:dynamic *log* nil)

(defn- log [& args]
  (when (thread-bound? #'*log*)
    (set! *log* (conj *log* args))))

(defn- ordering
  "Given an ordered collection of messages, returns a map from the
  head of each message to its index position in the collection."
  [log]
  (into {} (map-indexed (fn [i [message & _]] [message i]) log)))

(defn before?
  "In the collection of messages, does the message beginning with
  symbol a come before the message begging with symbol b?"
  [log sym-a sym-b]
  (let [order (ordering log)]
    (< (get order sym-a) (get order sym-b))))

(defn started? [component]
  (true? (::started? component)))

(defn stopped? [component]
  (false? (::started? component)))

(defrecord ComponentA [state]
  component/Lifecycle
  (start [this]
    (log 'ComponentA.start this)
    (assoc this ::started? true))
  (stop [this]
    (log 'ComponentA.stop this)
    (assoc this ::started? false)))

(defn component-a []
  (->ComponentA (rand-int Integer/MAX_VALUE)))

(defrecord ComponentB [state a]
  component/Lifecycle
  (start [this]
    (log 'ComponentB.start this)
    (assert (started? a))
    (assoc this ::started? true))
  (stop [this]
    (log 'ComponentB.stop this)
    (assert (started? a))
    (assoc this ::started? false)))

(defn component-b []
  (component/using
    (map->ComponentB {:state (rand-int Integer/MAX_VALUE)})
    [:a]))

(defrecord ComponentC [state a b]
  component/Lifecycle
  (start [this]
    (log 'ComponentC.start this)
    (assert (started? a))
    (assert (started? b))
    (assoc this ::started? true))
  (stop [this]
    (log 'ComponentC.stop this)
    (assert (started? a))
    (assert (started? b))
    (assoc this ::started? false)))

(defn component-c []
  (component/using
    (map->ComponentC {:state (rand-int Integer/MAX_VALUE)})
    [:a :b]))

(defrecord ComponentD [state my-c b]
  component/Lifecycle
  (start [this]
    (log 'ComponentD.start this)
    (assert (started? b))
    (assert (started? my-c))
    (assoc this ::started? true))
  (stop [this]
    (log 'ComponentD.stop this)
    (assert (started? b))
    (assert (started? my-c))
    (assoc this ::started? false)))

(defn component-d []
  (map->ComponentD {:state (rand-int Integer/MAX_VALUE)}))

(defrecord ComponentE [state]
  component/Lifecycle
  (start [this]
    (log 'ComponentE.start this)
    (assoc this ::started? true))
  (stop [this]
    (log 'ComponentE.stop this)
    (assoc this ::started? false)))

(defn component-e []
  (map->ComponentE {:state (rand-int Integer/MAX_VALUE)}))

(defrecord System1 [d a e c b]  ; deliberately scrambled order
  component/Lifecycle
  (start [this]
    (log 'System1.start this)
    (component/start-system this (keys this)))
  (stop [this]
    (log 'System1.stop this)
    (component/stop-system this (keys this))))

(defn system-1 []
  (map->System1 {:a (component-a)
                 :b (component-b)
                 :c (component-c)
                 :d (component/using (component-d)
                      {:b :b
                       :my-c :c})
                 :e (component-e)}))

(defmacro with-log [& body]
  `(binding [*log* []]
     ~@body
     *log*))

(deftest components-start-in-order
  (let [log (with-log (component/start (system-1)))]
    (are [k1 k2] (before? log k1 k2)
         'ComponentA.start 'ComponentB.start
         'ComponentA.start 'ComponentC.start
         'ComponentB.start 'ComponentC.start
         'ComponentC.start 'ComponentD.start
         'ComponentB.start 'ComponentD.start)))

(deftest all-components-started
  (let [system (component/start (system-1))]
    (doseq [component (vals system)]
      (is (started? component)))))

(deftest all-components-stopped
  (let [system (component/stop (component/start (system-1)))]
    (doseq [component (vals system)]
      (is (stopped? component)))))

(deftest dependencies-satisfied
  (let [system (component/start (component/start (system-1)))]
    (are [keys] (started? (get-in system keys))
         [:b :a]
         [:c :a]
         [:c :b]
         [:d :my-c])))

(defrecord ErrorStartComponentC [state a b]
  component/Lifecycle
  (start [this]
    (throw (ex-info "Boom!" {:state state})))
  (stop [this]
    this))

(defn error-start-c []
  (component/using
    (map->ErrorStartComponentC {:state (rand-int Integer/MAX_VALUE)})
    [:a :b]))

(deftest error-thrown-with-partial-system
  (let [ex (try (component/start (assoc (system-1)
                                   :c (error-start-c)))
                (catch Exception e e))]
    (is (started? (-> ex ex-data :system :b :a)))))
