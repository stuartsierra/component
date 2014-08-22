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

;; No-op implementation if one is not defined.
(extend-protocol Lifecycle
  java.lang.Object
  (start [this]
    this)
  (stop [this]
    this))

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
                      {:reason ::invalid-dependencies
                       :component component
                       :dependencies dependencies})))))

(defn system-using
  "Associates dependency metadata with multiple components in the
  system. dependency-map is a map of keys in the system to maps or
  vectors specifying the dependencies of the component at that key in
  the system, as per 'using'."
  [system dependency-map]
  (reduce-kv
   (fn [system key dependencies]
     (let [component (get system key)]
       (when-not component
         (throw (ex-info (str "Missing component " key " from system")
                         {:reason ::missing-component
                          :system-key key
                          :system system})))
       (assoc system key (using component dependencies))))
   system
   dependency-map))

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

(defn- assoc-dependency [system component dependency-key system-key]
  (let [dependency (get system system-key)]
    (when-not dependency
      (throw (ex-info (str "Missing dependency " dependency-key
                           " in " (.getName (class component))
                           " expected in system at " system-key)
                      {:reason ::missing-dependency
                       :system-key system-key
                       :dependency-key dependency-key
                       :component component
                       :system system})))
    (assoc component dependency-key dependency)))

(defn- assoc-dependencies [component system]
  (reduce-kv #(assoc-dependency system %1 %2 %3)
             component
             (dependencies component)))

(defn- try-action [component system key f args]
  (try (apply f component args)
       (catch Throwable t
         (throw (ex-info (str "Error in component " key
                              " in system " (.getName (class system))
                              " calling " f)
                         {:reason ::component-function-threw-exception
                          :function f
                          :system-key key
                          :component component
                          :system system}
                         t)))))

(defn- get-component [system key]
  (or (get system key)
      (throw (ex-info (str "Missing component " key " from system")
                      {:reason ::missing-component
                       :system-key key
                       :system system}))))

(defn update-system
  "Invokes (apply f component args) on each of the components at
  component-keys in the system, in dependency order. Before invoking
  f, assoc's updated dependencies of the component."
  [system component-keys f & args]
  (let [graph (dependency-graph system component-keys)]
    (reduce (fn [system key]
              (assoc system key
                     (-> (get-component system key)
                         (assoc-dependencies system)
                         (try-action system key f args))))
            system
            (sort (dep/topo-comparator graph) component-keys))))

(defn update-system-reverse
  "Like update-system but operates in reverse dependency order."
  [system component-keys f & args]
  (let [graph (dependency-graph system component-keys)]
    (reduce (fn [system key]
              (assoc system key
                     (-> (get-component system key)
                         (assoc-dependencies system)
                         (try-action system key f args))))
            system
            (reverse (sort (dep/topo-comparator graph) component-keys)))))

(defn start-system
  "Recursively starts components in the system, in dependency order,
  assoc'ing in their dependencies along the way. component-keys is a
  collection of keys (order doesn't matter) in the system specifying
  the components to start, defaults to all keys in the system."
  ([system]
     (start-system system (keys system)))
  ([system component-keys]
     (update-system system component-keys #'start)))

(defn stop-system
  "Recursively stops components in the system, in reverse dependency
  order. component-keys is a collection of keys (order doesn't matter)
  in the system specifying the components to stop, defaults to all
  keys in the system."
  ([system]
     (stop-system system (keys system)))
  ([system component-keys]
     (update-system-reverse system component-keys #'stop)))

(defrecord SystemMap []
  Lifecycle
  (start [system]
    (start-system system))
  (stop [system]
    (stop-system system)))

(defmethod clojure.core/print-method SystemMap
  [system ^java.io.Writer writer]
  (.write writer "#<SystemMap>"))

(defn system-map
  "Returns a system constructed of key/value pairs. The system has
  default implementations of the Lifecycle 'start' and 'stop' methods
  which recursively start/stop all components in the system.

  System maps print as #<SystemMap> to avoid overwhelming the printer
  with large objects. As a consequence, printed system maps cannot be
  'read'. To disable this behavior and print system maps like normal
  records, call
  (remove-method clojure.core/print-method com.stuartsierra.component.SystemMap)"
  [& keyvals]
  ;; array-map doesn't check argument length (CLJ-1319)
  (when-not (even? (count keyvals))
    (throw (IllegalArgumentException.
            "system-map requires an even number of arguments")))
  (map->SystemMap (apply array-map keyvals)))

(defn ex-component?
  "True if the java.lang.Throwable has ex-data indicating it was
  thrown by something in the com.stuartsierra.component namespace."
  [throwable]
  (let [{:keys [reason]} (ex-data throwable)]
    (and (keyword? reason)
         (= "com.stuartsierra.component"
            (namespace reason)))))

(defn ex-without-components
  "If the java.lang.Throwable has ex-data provided by the
  com.stuartsierra.component namespace, returns a new exception
  instance with the :component and :system removed from its ex-data.
  Preserves the message, cause, and stacktrace of the original
  throwable. If the throwable was not created by this namespace,
  returns it unchanged. Use this when you want to catch and rethrow
  exceptions without including the full component or system."
  [^Throwable throwable]
  (if (ex-component? throwable)
    (let [^Throwable ex
          (ex-info (.getMessage throwable)
                   (dissoc (ex-data throwable) :component :system)
                   (.getCause throwable))]
      ;; .getStackTrace should never be null, but .setStackTrace
      ;; doesn't allow null, so we'll be careful
      (when-let [stacktrace (.getStackTrace throwable)]
        (.setStackTrace ex stacktrace))
      ex)
    throwable))

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
