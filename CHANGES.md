# Component Change Log


## 0.3.x series

### Git master branch at 0.3.3-SNAPSHOT

### Version [0.3.2] released on December 12, 2016

  * Fix incorrect base class for extending Lifecycle (commit 69e62854c7)

### Version [0.3.1] released on November 28, 2015

  * Fix #40, incorrect values for `ex-data` keys in missing-dependency
    error

### Version [0.3.0] release on September 18, 2015

  * API-compatible with 0.2.x

  * Minimum Clojure version 1.7.0 for Conditional Read

  * Added ClojureScript support via Conditional Read



## 0.2.x series

### Version [0.2.3] released on March 3, 2015

  * More-specific error message when a component returns `nil` from
    `start` or `stop`: see [commit fb891500]

### Version [0.2.2] released on September 7, 2014

  * System maps print as `#<SystemMap>` to avoid trying to print huge
    objects in the REPL

  * Add error helpers `ex-component?` and `ex-without-components`

  * Change `:component-key` to `:system-key` in `ex-data` maps:
    breaking change for code which depended on the value of
    `:component-key`

  * Add `:system-key` to `ex-data` map from `try-action`

  * Minor changes to exception message strings

  * Leiningen profiles / aliases to test on all supported Clojure
    versions

### Version [0.2.1] released on December 17, 2013

  * Add generic `system-map`

  * More descriptive messages on exceptions

  * Add arity-1 versions of `start-system` and `stop-system` that
    default to all keys in the system

### Version [0.2.0] released on November 20, 2013

  * API compatible with 0.1.0

  * Some exception messages changed

  * Added default no-op implementation of Lifecycle protocol

  * Added `update-system` and `update-system-reverse`

  * Redefined `start-system` and `stop-system` in terms of these

  * `stop-system` now assoc's dependencies just like `start-system`



## 0.1.x series

### Version [0.1.0] released on October 28, 2013


[0.3.1]: https://github.com/stuartsierra/component/tree/component-0.3.1
[0.3.0]: https://github.com/stuartsierra/component/tree/component-0.3.0
[0.2.3]: https://github.com/stuartsierra/component/tree/component-0.2.3
[0.2.2]: https://github.com/stuartsierra/component/tree/component-0.2.2
[0.2.1]: https://github.com/stuartsierra/component/tree/component-0.2.1
[0.2.0]: https://github.com/stuartsierra/component/tree/component-0.2.0
[0.1.0]: https://github.com/stuartsierra/component/tree/component-0.1.0

[commit fb891500]: https://github.com/stuartsierra/component/commit/fb891500506b048bd8d9d689dfd3ed8c0e940944

[dependency]: https://github.com/stuartsierra/dependency
[tools.namespace]: https://github.com/clojure/tools.namespace
