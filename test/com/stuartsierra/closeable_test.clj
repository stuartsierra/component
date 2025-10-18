(ns com.stuartsierra.closeable-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]))

;; Compiling these tests should not generate reflection warnings
(set! *warn-on-reflection* true)

(defrecord TestComponent [state]
  component/Lifecycle
  (start [this]
    (reset! state :started)
    this)
  (stop [this]
    (reset! state :stopped)
    this))

(deftest t-closeable
  (testing "SystemMap can be stopped via java.io.Closeable"
    (let [state (atom nil)
          test-component (->TestComponent state)
          system (component/system-map :test test-component)]
      (with-open [^java.io.Closeable system (component/start system)]
        (is (instance? java.io.Closeable system))
        (is (= :started @state)))
      (is (= :stopped @state)))))

(deftest t-autocloseable
  (testing "SystemMap can be stopped via java.lang.AutoCloseable"
    (let [state (atom nil)
          test-component (->TestComponent state)
          system (component/system-map :test test-component)]
      (with-open [^java.lang.AutoCloseable system (component/start system)]
        (is (instance? java.lang.AutoCloseable system))
        (is (= :started @state)))
      (is (= :stopped @state)))))
