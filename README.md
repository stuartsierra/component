# component

'Component' is a tiny Clojure framework for managing the lifecycle of
software components which have runtime state.

This is primarily a design pattern with a few helper functions. It can
be seen as a style of dependency injection using immutable data
structures.




## Releases and Dependency Information

* I publish releases to [Clojars]

* Latest stable release is [0.2.1](https://github.com/stuartsierra/component/tree/component-0.2.1)

* [All releases](https://clojars.org/com.stuartsierra/component)

[Leiningen] dependency information:

    [com.stuartsierra/component "0.2.1"]

[Maven] dependency information:

    <dependency>
      <groupId>com.stuartsierra</groupId>
      <artifactId>component</artifactId>
      <version>0.2.1</version>
    </dependency>

[Gradle] dependency information:

    compile "com.stuartsierra:component:0.2.1"

[Clojars]: http://clojars.org/
[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/
[Gradle]: http://www.gradle.org/



## API Stability

'Component' is new, experimental, subject to change, alpha, beta,
gamma, delta, blah blah blah.

I will make an effort not to break backwards compability between
releases at the same 0.N version, e.g. 0.2.X and 0.2.Y



## Dependencies and Compatibility

I have successfully tested 'Component' with Clojure versions
1.4.0 and 1.5.1.

'Component' uses my [dependency] library.

[dependency]: https://github.com/stuartsierra/dependency



## Introduction

For the purposes of this framework, a *component* is a collection of
functions or procedures which share some runtime state.

Some examples of components:

* Database access: query and insert functions sharing a database
  connection

* External API service: functions to send and retrieve data sharing an
  HTTP connection pool

* Web server: functions to handle different routes sharing all the
  runtime state of the web application, such as a session store

* In-memory cache: functions to get and set data in a shared mutable
  reference such as a Clojure Atom or Ref

A *component* is similar in spirit to the definition of an *object* in
Object-Oriented Programming.

Clojure is not an object-oriented programming language, so we do not
have to cram everything into the OO model. Most functions are just
functions. But real-world applications need to manage state.
Components are a tool to help with that.


### Advantages of the Component Model

Large applications often consist of many stateful processes which must
be started and stopped in a particular order. The component model
makes those relationships explicit and declarative, instead of
implicit in imperative code.

Components provide some basic guidance for structuring a Clojure
application, with boundaries between different parts of a system.
Components offer some encapsulation, in the sense of grouping together
related entities. Each component receives references only to the
things it needs, avoiding unnecessary shared state. Instead of
reaching through multiple levels of nested maps, a component can have
everything it needs at most one map lookup away.

Instead of having mutable state (atoms, refs, etc.) scattered
throughout different namespaces, all the stateful parts of an
application can be gathered together. In some cases, using components
may eliminate the need for mutable references altogether, for example
to store the "current" connection to a resource such as a database. At
the same time, having all state reachable via a single "system" object
makes it easy to reach in and inspect any part of the application from
the REPL.

The component dependency model makes it easy to swap in "stub" or
"mock" implementations of a component for testing purposes, without
relying on time-dependent constructs, such as `with-redefs` or
`binding`, which are often subject to race conditions in
multi-threaded code.

Having a coherent way to set up and tear down **all** the state
associated with an application enables rapid development cycles
without restarting the JVM. It can also make unit tests faster and
more independent, since the cost of creating and starting a system is
low enough that every test can create a new instance of the system.


### Disadvantages of the Component Model

First and foremost, this framework works best when all parts of an
application follow the same pattern. It is not easy to retrofit the
component model to an existing application without major refactoring.

In particular, the 'component' code assumes that all application state
is passed as arguments to the functions that use it. As a result, this
framework may not work well with code which relies on global or
singleton references.

For small applications, declaring the dependency relationships among
components may actually be harder than manually starting all the
components in the correct order. You can still use the 'Lifecycle'
protocol without using the dependency-injection features, but the
added value of 'component' in that case is small.

The "system object" produced by this framework is a large and complex
map with a lot of duplication. The same component may appear in
multiple places in the map. The actual memory cost of this duplication
is negligible due to persistent data structures, but the system map is
typically too large to inspect visually.

You must explicitly specify all the dependency relationships among
components: the code cannot discover these relationships
automatically.

Finally, the 'component' library forbids cyclic dependencies among
components. I believe that cyclic dependencies usually indicate
architectural flaws and can be eliminated by restructuring the
application. In the rare case where a cyclic dependency cannot be
avoided, you can use mutable references to manage it, but this is
outside the scope of 'component'.




## Usage

```clojure
(ns com.example.your-application
  (:require [com.stuartsierra.component :as component]))
```


### Creating Components

To create a component, define a Clojure record that implements the
`Lifecycle` protocol.

```clojure
(defrecord Database [host port connection]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    ;; In the 'start' method, initialize this component
    ;; and start it running. For example, connect to a
    ;; database, create thread pools, or initialize shared
    ;; state.
    (let [conn (connect-to-database host port)]
      ;; Return an updated version of the component with
      ;; the run-time state assoc'd in.
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    (.close connection)
    ;; Return the component, optionally modified.
    component))
```

Optionally, provide a constructor function that takes arguments for
the essential configuration parameters of the component, leaving the
runtime state blank.

```clojure
(defn new-database [host port]
  (map->Database {:host host :port port}))
```

Define the functions implementing the behavior of the
component to take the component itself as an argument.

```clojure
(defn get-user [database username]
  (execute-query (:connection database)
    "SELECT * FROM users WHERE username = ?"
    username))

(defn add-user [database username favorite-color]
  (execute-insert (:connection database)
    "INSERT INTO users (username, favorite_color)"
    username favorite-color))
```

Define other components in terms of the components on which they
depend.

```clojure
(defrecord ExampleComponent [options cache database scheduler]
  component/Lifecycle

  (start [this]
    (println ";; Starting ExampleComponent")
    ;; In the 'start' method, a component may assume that its
    ;; dependencies are available and have already been started.
    (assoc this :admin (get-user database "admin")))

  (stop [this]
    (println ";; Stopping ExampleComponent")
    ;; Likewise, in the 'stop' method, a component may assume that its
    ;; dependencies will not be stopped until AFTER it is stopped.
    this))
```

Not all the dependencies need to be supplied at construction time. In
general, the constructor should not depend on other components being
available or started.

A component's runtime dependencies will be injected into it by the
system which contains it: see the next section.

```clojure
(defn example-component [config-options]
  (map->ExampleComponent {:options config-options
                          :cache (atom {})}))
```


### Systems

Components are composed into systems. A system is a component which
knows how to start and stop other components. It is also responsible
for injecting dependencies into the components which need them.

A system can use the helper functions `start-system` and `stop-system`,
which take a set of keys naming components in the system to be
started/stopped. Order of the keys doesn't matter here.

```clojure
(def example-system-components [:scheduler :app :db])

(defrecord ExampleSystem [config-options db scheduler app]
  component/Lifecycle
  (start [this]
    (component/start-system this example-system-components))
  (stop [this]
    (component/stop-system this example-system-components)))
```

When constructing the system, specify the dependency relationships
among components with the `using` function.

```clojure
(defn example-system [config-options]
  (let [{:keys [host port]} config-options]
    (map->ExampleSystem
      {:config-options config-options
       :db (new-database host port)
       :scheduler (new-scheduler)
       :app (component/using
              (example-component config-options)
              {:database  :db
               :scheduler :scheduler})})))
```

`using` takes a component and a map telling the system where to
find that component's dependencies. Keys in the map are the keys in
the component record itself, values are the map are the
corresponding keys in the system record.

    {:component-key :system-key}

In the example above:

       (component/using
         (example-component config-options)
         {:database  :db
          :scheduler :scheduler})
    ;;     ^          ^
    ;;     |          |
    ;;     |          \- Keys in the ExampleSystem record
    ;;     |
    ;;     \- Keys in the ExampleComponent record

Based on this information (stored as metadata on the component
records) the `start-system` function will construct a dependency graph
of the components and start them all in the correct order.

Before starting each component, `start-system` will `assoc` its
dependencies based on the metadata provided by `using`.

Again using the example above, the ExampleComponent would be started
as if by:

```
(-> example-component
    (assoc :database (:db system))
    (assoc :scheduler (:scheduler system))
    (start))
```

Optionally, if the keys in the system map are the same as the keys in
the component map, `using` can take a vector of those keys instead of
a map. If you know the names of all the components in your system, you
can add the metadata in the component's constructor:

```clojure
(defrecord AnotherComponent [component-a component-b])

(defrecord AnotherSystem [component-a component-b component-c])

(defn another-component []
  (component/using
    (map->AnotherComponent {})
    [:component-a :component-b]))
```

Alternately, component dependencies can be specified all at once for
all components in the system with `system-using`, which takes a map
from component names to their dependencies.

```clojure
(defn example-system [config-options]
  (let [{:keys [host port]} config-options]
    (-> (map->ExampleSystem
          {:config-options config-options
           :db (new-database host port)
           :scheduler (new-scheduler)
           :app (example-component config-options)})
        (component/system-using
          {:app {:database  :db
                 :scheduler :scheduler}}))))
```


### Example REPL Session

```clojure
(def system (example-system {:host "dbhost.com" :port 123}))
;;=> #'examples/system

(alter-var-root #'system component/start)
;; Starting database
;; Opening database connection
;; Starting scheduler
;; Starting ExampleComponent
;; execute-query
;;=> #examples.ExampleSystem{ ... }

(alter-var-root #'system component/stop)
;; Stopping ExampleComponent
;; Stopping scheduler
;; Stopping database
;; Closing database connection
;;=> #examples.ExampleSystem{ ... }
```


### Errors

While starting/stopping a system, if any component's `start` or `stop`
method throws an exception, the `start-system` or `stop-system`
function will catch and wrap it in an `ex-info` exception with the
following keys in its `ex-data` map:

* `:system` is the current system, including all the components which
  have already been started.

* `:component` is the component which caused the exception, with its
  dependencies already `assoc`'d in.

The original exception which the component threw is available as
`.getCause` on the exception.

'Component' makes no attempt to recover from errors in a component,
but you can use the system attached to the exception to clean up any
partially-constructed state.

You may find it useful to define your `start` and `stop` methods to be
idempotent, i.e., to have effect only if the component is not already
started or stopped.

```clojure
(defrecord IdempotentDatabaseExample [host port connection]
  component/Lifecycle
  (start [this]
    (if connection  ; already started
      this
      (assoc this :connection (connect host port))))
  (stop [this]
    (if (not connection)  ; already stopped
      this
      (do (.close connection)
          (assoc this :connection nil)))))
```

'Component' does not require that stop/start be idempotent, but
idempotence can make it easier to clean up state after an error,
because you can call `stop` indiscriminately on everything.

In addition, you could wrap the body of `stop` in a try/catch that
ignores all exceptions. That way, errors stopping one component will
not prevent other components from shutting down cleanly.

```clojure
(try (.close connection)
  (catch Throwable t
    (log/warn t "Error when stopping component")))
```


### Stateless Components

There is a default implementation of Lifecycle which is a no-op. If
you omit the `Lifecycle` protocol from a component, it can still
participate in the dependency injection process.

You cannot omit just one of the `start` or `stop` methods: any
component which implements `Lifecycle` must supply both.


### Reloading

I developed this pattern in combination with my "reloaded" [workflow].
For development, I might create a `user` namespace like this:

[workflow]: http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

```clojure
(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [examples :as app]))

(def system nil)

(defn init []
  (alter-var-root #'system
    (constantly (app/example-system {:host "dbhost.com" :port 123}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
```


### Production

In the deployed or production version of my application, I typically
have a "main" function that creates and starts the top-level system.

```clojure
(ns com.example.application.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [examples :as app]))

(defn -main [& args]
  (let [[host port] args]
    (component/start
      (app/example-system {:host host :port port}))))
```

This also works well in conjunction with command-line drivers such as
[Apache Commons Daemon](http://commons.apache.org/proper/commons-daemon/).


### Usage Notes

The top-level "system" record is intended to be used exclusively for
starting and stopping other components. No component in the system
should depend directly on its parent system.

I do not intend that application functions should receive the
top-level system as an argument. Rather, functions are defined in
terms of components. Each component receives references only to the
components on which it depends.

The "application" or "business logic" may itself be represented by a
component.

Components may, of course, implement other protocols besides
`Lifecycle`.

Different implementations of a component (for example, a stub version
for testing) can be injected into a system with `assoc` before calling
`start`.

Systems may be composed into larger systems, although I have not yet
found a need for this.


### Notes for Library Authors

'Component' is intended as a tool for applications, not resuable
libraries. I do not believe that a general-purpose library should
impose any particular framework on the application which uses it.

That said, library authors can make it trivially easy for applications
to use their libraries in combination with the 'Component' pattern by
following these guidelines:

* Never create global mutable state (for example, an Atom or Ref
  stored in a `def`).

* Never rely on dynamic binding to convey state (for example, the
  "current" database connection).

* Never perform side effects at the top level of a source file.

* Encapsulate all the runtime state needed by the library in a single
  data structure.

* Provide functions to construct and destroy that data structure.

* Take the encapsulated runtime state as an argument to any library
  functions which depend on it.


### Customization

The `start-system` and `stop-system` functions are just special cases
of two other functions, `update-system` and `update-system-reverse`.
(Added in 0.2.0)

You could, for example, define your own lifecycle functions as new
protocols. You don't even have to use protocols and records;
multimethods and ordinary maps would work as well.

Both `update-system` and `update-system-reverse` take a function as
an argument and call it on each component in the system. Along the
way, they `assoc` in the updated dependencies of each component.

The `update-system` function iterates over the components in
dependency order (a component will be called *after* its dependencies)
the `update-system-reverse` function goes in reverse dependency order
(a component will be called *before* its dependencies).

Calling `update-system` with the `identity` function is equivalent to
doing just the dependency injection part of 'component'.



## References / More Information

* [tools.namespace](https://github.com/clojure/tools.namespace)
* [On the Perils of Dynamic Scope](http://stuartsierra.com/2013/03/29/perils-of-dynamic-scope).
* [Clojure in the Large](http://www.infoq.com/presentations/Clojure-Large-scale-patterns-techniques) (video)
* [Relevance Podcast Episode 32](http://thinkrelevance.com/blog/2013/05/29/stuart-sierra-episode-032) (audio)
* [My Clojure Workflow, Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
* [reloaded](https://github.com/stuartsierra/reloaded) Leiningen template
* [Lifecycle Composition](http://stuartsierra.com/2013/09/15/lifecycle-composition)



## Change Log

* Version 0.2.2-SNAPSHOT (current Git master branch)
* Version [0.2.1] released on December 17, 2013
  * Add generic `system-map`
  * More descriptive messages on exceptions
  * Add arity-1 versions of `start-system` and `stop-system` that
    default to all keys in the system
* Version [0.2.0] released on November 20, 2013
  * API compatible with 0.1.0
  * Some exception messages changed
  * Added default no-op implementation of Lifecycle protocol
  * Added `update-system` and `update-system-reverse`
  * Redefined `start-system` and `stop-system` in terms of these
  * `stop-system` now assoc's dependencies just like `start-system`
* Version [0.1.0] released on October 28, 2013

[0.2.1]: https://github.com/stuartsierra/component/tree/component-0.2.1
[0.2.0]: https://github.com/stuartsierra/component/tree/component-0.2.0
[0.1.0]: https://github.com/stuartsierra/component/tree/component-0.1.0



## Copyright and License

The MIT License (MIT)

Copyright © 2013 Stuart Sierra

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
