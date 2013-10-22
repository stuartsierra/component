# component

'Component' is a tiny Clojure framework for managing the lifecycle of
software components with runtime state which must be manually
initialized and destroyed.

This is primarily a design pattern with a few helper functions.



## Releases and Dependency Information

No releases yet. Run `lein install` in this directory to install the
current SNAPSHOT version.

* Releases will be published to [Clojars]

* Latest stable release is TODO_LINK

* All released versions TODO_LINK

[Leiningen] dependency information:

    [com.stuartsierra/component "0.1.0-SNAPSHOT"]

[Maven] dependency information:

    <dependency>
      <groupId>com.stuartsierra/groupId>
      <artifactId>component</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>

[Clojars]: http://clojars.org/
[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/



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
have to cram everything into this model. Some functions are just
functions. But real-world applications need to manage state.
Components are a tool to help with that.



## Usage

See file `dev/examples.clj`




## Change Log

* Version 0.1.0-SNAPSHOT



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
