# Component

'Component' is a tiny Clojure framework for managing the lifecycle and
dependencies of software components which have runtime state.

This is primarily a design pattern with a few helper functions. It can
be seen as a style of dependency injection using immutable data
structures.

See the [video from Clojure/West 2014](https://www.youtube.com/watch?v=13cmHf_kt-Q) (YouTube, 40 minutes)


## Releases and Dependency Information

* I publish releases to [Clojars]

* Latest stable release is [0.4.0](https://github.com/stuartsierra/component/tree/component-0.4.0)

* [All releases](https://clojars.org/com.stuartsierra/component)

[Leiningen] dependency information:

    [com.stuartsierra/component "0.4.0"]

[Maven] dependency information:

    <dependency>
      <groupId>com.stuartsierra</groupId>
      <artifactId>component</artifactId>
      <version>0.4.0</version>
    </dependency>

[Gradle] dependency information:

    compile "com.stuartsierra:component:0.4.0"

[Clojars]: http://clojars.org/
[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/
[Gradle]: http://www.gradle.org/



## Dependencies and Compatibility

Starting with version 0.3.0 of 'Component', Clojure or ClojureScript
version 1.7.0 or higher is required for Conditional Read support.

Version 0.2.3 of 'Component' is compatible with
Clojure versions 1.4.0 and higher.

'Component' requires my [dependency] library

[dependency]: https://github.com/stuartsierra/dependency



## Discussion

Please post questions on the [Clojure Mailing List](https://groups.google.com/forum/#!forum/clojure)



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
Object-Oriented Programming. This does not alter the primacy of pure
functions and immutable data structures in Clojure as a language. Most
functions are just functions, and most data are just data. Components
are intended to help manage stateful resources within a functional
paradigm.


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

In particular, the 'component' library assumes that all application
state is passed as arguments to the functions that use it. As a
result, this framework may be awkward to use with code which relies on
global or singleton references.

For small applications, declaring the dependency relationships among
components may actually be more work than manually starting all the
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
    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (assoc component :connection nil)))
```

Optionally, provide a constructor function that takes arguments for
the essential configuration parameters of the component, leaving the
runtime state blank.

```clojure
(defn new-database [host port]
  (map->Database {:host host :port port}))
```

Define the functions implementing the behavior of the component to
take an **instance** of the component as an argument.

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

**Do not pass component dependencies in a constructor.**
Systems are responsible for injecting runtime dependencies into the
components they contain: see the next section.

```clojure
(defn example-component [config-options]
  (map->ExampleComponent {:options config-options
                          :cache (atom {})}))
```


### Systems

Components are composed into systems. A system is a component which
knows how to start and stop other components. It is also responsible
for injecting dependencies into the components which need them.

The easiest way to create a system is with the `system-map` function,
which takes a series of key/value pairs just like the `hash-map` or
`array-map` constructors. Keys in the system map are keywords. Values
in the system map are *instances* of components, usually records or
maps.

```clojure
(defn example-system [config-options]
  (let [{:keys [host port]} config-options]
    (component/system-map
      :db (new-database host port)
      :scheduler (new-scheduler)
      :app (component/using
             (example-component config-options)
             {:database  :db
              :scheduler :scheduler}))))
```

Specify the dependency relationships among components with the `using`
function. `using` takes a component and a collection of keys naming
that component's dependencies.

If the component and the system use the same keys, then you can
specify dependencies as a *vector* of keys:

```clojure
    (component/system-map
      :database (new-database host port)
      :scheduler (new-scheduler)
      :app (component/using
             (example-component config-options)
             [:database :scheduler]))
             ;; Both ExampleComponent and the system have
             ;; keys :database and :scheduler
```

If the component and the system use *different* keys, then specify
them as a map of `{:component-key :system-key}`.
That is, the `using` keys match the keys in the component,
the values match keys in the system.

```clojure
    (component/system-map
      :db (new-database host port)
      :sched (new-scheduler)
      :app (component/using
             (example-component config-options)
             {:database  :db
              :scheduler :sched}))
        ;;     ^          ^
        ;;     |          |
        ;;     |          \- Keys in the system map
        ;;     |
        ;;     \- Keys in the ExampleComponent record
```

The system map provides its own implementation of the Lifecycle
protocol which uses this dependency information (stored as metadata on
each component) to start the components in the correct order.

Before starting each component, the system will `assoc` its
dependencies based on the metadata provided by `using`.

Again using the example above, the ExampleComponent would be started
*as if* you did this:

```
(-> example-component
    (assoc :database (:db system))
    (assoc :scheduler (:sched system))
    (start))
```

Stop a system by calling the `stop` method on it. This will stop each
component, in *reverse* dependency order, and then re-assoc the
dependencies of each component. **Note:** `stop` is not the exact
inverse of `start`; component dependencies will still be associated.

It doesn't matter *when* you associate dependency metadata on a
component, as long as it happens before you call `start`. If you know
the names of all the components in your system in advance, you could
choose to add the metadata in the component's constructor:

```clojure
(defrecord AnotherComponent [component-a component-b])

(defrecord AnotherSystem [component-a component-b component-c])

(defn another-component []   ; constructor
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
    (-> (component/system-map
          :config-options config-options
          :db (new-database host port)
          :sched (new-scheduler)
          :app (example-component config-options))
        (component/system-using
          {:app {:database  :db
                 :scheduler :sched}}))))
```


### Entry Points in Production

The 'component' library does not dictate how you store the system map
or use the components it contains. That's up to you.

The typical approach differs in development and production:

In **production**, the system map is ephemeral. It is used to start
all the components running, then it is discarded.

When your application starts, for example in a `main` function,
construct an instance of the system and call `component/start` on it.
Then hand off control to one or more components that represent the
"entry points" of your application.

For example, you might have a web server component that starts
listening for HTTP requests, or an event loop component that waits for
input. Each of these components can create one or more threads in its
Lifecycle `start` method. Then `main` could be as trivial as:

```clojure
(defn main [] (component/start (new-system)))
```

**Note:** You will still need to keep the main thread of your
application running to prevent the JVM from shutting down. One way is
to block the main thread waiting for some signal to shut down; another
way is to `Thread/join` the main thread to one of your components'
threads.

This also works well in conjunction with command-line drivers such as
[Apache Commons Daemon](http://commons.apache.org/proper/commons-daemon/).


### Entry Points for Development

In **development**, it is useful to have a reference to the system map
to examine it from the REPL.

The easiest way to do this is to `def` a Var to hold the system map in
a development namespace. Use `alter-var-root` to start and stop it.

Example REPL session:

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

See the [reloaded] template for a more elaborate example.

[reloaded]: https://github.com/stuartsierra/reloaded


### Web Applications

Many Clojure web frameworks and tutorials are designed around an
assumption that a "handler" function exists as a global `defn`,
without any context. With this assumption, there is no easy way to use
any application-level context in the handler without making it also a
global `def`.

The 'component' approach assumes that any "handler" function receives
its state/context as an argument, without depending on any global state.

To reconcile these two approaches, create the "handler" function as a
*closure* over one or more components in a Lifecycle `start` method.
Pass this closure to the web framework as the "handler".

Most web frameworks or libraries that have a static `defroutes` or
similar macro will provide an equivalent non-static `routes` which can
be used to create a closure.

It might look something like this:

```clojure
(defn app-routes
  "Returns the web handler function as a closure over the
  application component."
  [app-component]
  ;; Instead of static 'defroutes':
  (web-framework/routes
   (GET "/" request (home-page app-component request))
   (POST "/foo" request (foo-page app-component request))
   (not-found "Not Found")))

(defrecord WebServer [http-server app-component]
  component/Lifecycle
  (start [this]
    (assoc this :http-server
           (web-framework/start-http-server
             (app-routes app-component))))
  (stop [this]
    (stop-http-server http-server)
    this))

(defn web-server
  "Returns a new instance of the web server component which
  creates its handler dynamically."
  []
  (component/using (map->WebServer {})
                   [:app-component]))
```


## More Advanced Usage

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

The 'Component' library makes no attempt to recover from errors in a
component, but you can use the `:system` attached to the exception to
clean up any partially-constructed state.

Since component maps may be large, with a lot of repetition, you
probably don't want to log or print this exception as-is. The
`ex-without-components` helper function will remove the larger objects
from an exception.

The `ex-component?` helper function tells you if an exception was
originated or wrapped by 'Component'.


### Idempotence

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

The 'Component' library does not require that stop/start be
idempotent, but idempotence can make it easier to clean up state after
an error, because you can call `stop` indiscriminately on everything.

In addition, you could wrap the body of `stop` in a try/catch that
ignores all exceptions. That way, errors stopping one component will
not prevent other components from shutting down cleanly.

```clojure
(try (.close connection)
  (catch Throwable t
    (log/warn t "Error when stopping component")))
```


### Stateless Components

The default implementation of `Lifecycle` is a no-op. If you omit the
`Lifecycle` protocol from a component, it can still participate in the
dependency injection process.

Components which do not need a lifecycle can be ordinary Clojure maps.

You **cannot** omit just one of the `start` or `stop` methods: any
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


## Usage Notes


### Do not pass the system around

The top-level "system" record is used only for starting and stopping
other components, and for convenience during interactive development.

See "Entry Points in ..." above.


### No function should take the entire system as an argument

Application functions should never receive the whole system as an
argument. This is unnecessary sharing of global state.

Rather, each function should be defined in terms of **at most one**
component.

If a function depends on several components, then it should have its
own component with dependencies on the things it needs.


### No component should be aware of the system which contains it

Each component receives references only to the components on which it
depends.


### Do not nest systems

It's technically possible to nest one `system-map` in another, but the
effects on dependencies are subtle and confusing.

Instead, give all your components unique keys and merge them into one
system.


### Other kinds of components

The "application" or "business logic" may itself be represented by one
or more components.

Component records may, of course, implement other protocols besides
`Lifecycle`.

Any type of object, not just maps and records, can be a component if
it has no lifecycle and no dependencies. For example, you could put a
bare Atom or core.async Channel in the system map where other
components can depend on it.


### Test doubles

Different implementations of a component (for example, a stub version
for testing) can be injected into a system with `assoc` before calling
`start`.


### Notes for Library Authors

'Component' is intended as a tool for applications, not resuable
libraries. I would not expect a general-purpose library to impose any
particular framework on the applications which use it.

That said, library authors can make it trivially easy for applications
to use their libraries in combination with the 'Component' pattern by
following these guidelines:

* Never create global mutable state (for example, an Atom or Ref
  stored in a `def`).

* Never rely on dynamic binding to convey state (for example, the
  "current" database connection) unless that state is necessarily
  confined to a single thread.

* Never perform side effects at the top level of a source file.

* Encapsulate all the runtime state needed by the library in a single
  data structure.

* Provide functions to construct and destroy that data structure.

* Take the encapsulated runtime state as an argument to any library
  functions which depend on it.


### Customization

A system map is just a record that implements the Lifecycle protocol
via two public functions, `start-system` and `stop-system`. These two
functions are just special cases of two other functions,
`update-system` and `update-system-reverse`. (Added in 0.2.0)

You could, for example, define your own lifecycle functions as new
protocols. You don't even have to use protocols and records;
multimethods and ordinary maps would work as well.

Both `update-system` and `update-system-reverse` take a function as
an argument and call it on each component in the system. Along the
way, they `assoc` in the updated dependencies of each component.

The `update-system` function iterates over the components in
dependency order: a component will be called *after* its dependencies.
The `update-system-reverse` function goes in reverse dependency order:
a component will be called *before* its dependencies.

Calling `update-system` with the `identity` function is equivalent to
doing just the dependency injection part of 'Component' without
`Lifecycle`.



## References / More Information

* [video from Clojure/West 2014](https://www.youtube.com/watch?v=13cmHf_kt-Q) (YouTube, 40 minutes)
* [tools.namespace](https://github.com/clojure/tools.namespace)
* [On the Perils of Dynamic Scope](http://stuartsierra.com/2013/03/29/perils-of-dynamic-scope)
* [Clojure in the Large](http://www.infoq.com/presentations/Clojure-Large-scale-patterns-techniques) (video)
* [Relevance Podcast Episode 32](http://thinkrelevance.com/blog/2013/05/29/stuart-sierra-episode-032) (audio)
* [My Clojure Workflow, Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
* [reloaded](https://github.com/stuartsierra/reloaded) Leiningen template
* [Lifecycle Composition](http://stuartsierra.com/2013/09/15/lifecycle-composition)



## Copyright and License

The MIT License (MIT)

Copyright © 2015 Stuart Sierra

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
