(ns com.stuartsierra.component
  (:require [com.stuartsierra.dependency :as dep]))

(defprotocol Lifecycle
  (start [component]
    "Begins operation of this component. Synchronous, does not return
  until the component is started. Returns an updated version of this
  component.")
  (stop [component]
    "Ceases operation of this component. Synchronous, does not return
  until the component is stopped. Returns an updated version of this
  component."))

(defn dependencies
  "Returns the map of other components on which this component depends."
  [component]
  (::dependencies (meta component) {}))

(defn using
  "Associates metadata with component describing the other components
  on which it depends. Component dependencies are specified as a map.
  Keys in the map correspond to keys in this component which must be
  provided by its containing system. Values in the map are the keys in
  the system at which those components may be found. Alternatively, if
  the keys are the same in both the component and its enclosing
  system, they may be specified as a vector of keys."
  [component dependencies]
  (vary-meta
   component update-in [::dependencies] (fnil merge {})
   (cond
    (map? dependencies)
      dependencies
    (vector? dependencies)
      (into {} (map (fn [x] [x x]) dependencies))
    :else
      (throw (ex-info "Dependencies must be a map or vector"
                      {:component component
                       :dependencies dependencies})))))

(defn dependency-graph
  "Returns a dependency graph, using the data structures defined in
  com.stuartsierra.dependency, for the components found by
  (select-keys system component-keys)"
  [system component-keys]
  (reduce-kv (fn [graph key component]
               (reduce #(dep/depend %1 key %2)
                       graph
                       (vals (dependencies component))))
             (dep/graph)
             (select-keys system component-keys)))

(defn start-system
  "Recursively starts components in the system, in dependency order."
  [system component-keys]
  (let [graph (dependency-graph system component-keys)]
    (reduce (fn [system key]
              (let [component (get system key)]
                (try
                  (assoc system key
                         (start
                          (reduce-kv (fn [component dependency-key system-key]
                                       (assoc component dependency-key
                                              (get system system-key)))
                                     component
                                     (dependencies component))))
                  (catch Throwable t
                    (throw (ex-info "Error starting component"
                                    {:component component
                                     :system system}
                                    t))))))
            system
            (sort (dep/topo-comparator graph) component-keys))))

(defn stop-system
  "Recursively stops components in the system, in reverse dependency
  order."
  [system component-keys]
  (let [graph (dependency-graph system component-keys)]
    (reduce (fn [system key]
              (let [component (get system key)]
                (try
                  (assoc system key (stop component))
                  (catch Throwable t
                    (throw (ex-info "Error stopping component"
                                    {:component component
                                     :system system}
                                    t))))))
            system
            (reverse (sort (dep/topo-comparator graph) component-keys)))))

;; Copyright © 2013 Stuart Sierra

;; Permission is hereby granted, free of charge, to any person obtaining a copy of
;; this software and associated documentation files (the "Software"), to deal in
;; the Software without restriction, including without limitation the rights to
;; use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
;; the Software, and to permit persons to whom the Software is furnished to do so,
;; subject to the following conditions:

;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.

;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
;; FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
;; COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
;; IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
;; CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
