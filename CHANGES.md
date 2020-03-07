# Component Change Log


## 1.0.x series

### Version [1.0.0] released on March 7, 2020

  * Modify Clojure dependency to be `:scope "provided"`
    to prevent spurious warnings when overridden by a
    later version. Note that this also prevents warnings
    when overridden by an **earlier, incompatible**
    version of Clojure. Component since version 0.3.0
    requires Clojure 1.7.0 or later.
    ([commit 5af4ad06]; reported by Dave Yarwood in [#65]).

  * Update 'dependency' library to version 1.0.0



## 0.4.x series

### Version [0.4.0] released on December 30, 2018

  * Add `:extend-via-metadata` to Lifecycle protocol
    for Clojure 1.10 [extend-via-metadata].
    Backwards-compatible with earlier Clojure versions
    ([commit 7824f551]; suggested by Joe Lane in [#62]).

  * **Not** backwards-compatible with ClojureScript versions
    before [ClojureScript 1.10.516]
    (reported by Ryan Schmukler in [#63]).



## 0.3.x series

### Version [0.3.2] released on December 12, 2016

  * Fix incorrect base class for extending Lifecycle
    ([commit 69e62854]; reported by Tim Pote)

### Version [0.3.1] released on November 28, 2015

  * Fix incorrect values for `ex-data` keys in missing-dependency
    error (reported by Howard Lewis Ship at [#40])

### Version [0.3.0] release on September 18, 2015

  * API-compatible with 0.2.x

  * Minimum Clojure version 1.7.0 for Conditional Read

  * Added ClojureScript support via Conditional Read



## 0.2.x series

### Version [0.2.3] released on March 3, 2015

  * More-specific error message when a component returns `nil` from
    `start` or `stop` ([commit fb891500]; reported by James Gatannah
    at [#17])

### Version [0.2.2] released on September 7, 2014

  * System maps print as `#<SystemMap>` to avoid trying to print huge
    objects in the REPL (reported by Howard Lewis Ship at [#9])

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


[1.0.0]: https://github.com/stuartsierra/component/tree/component-1.0.0
[0.4.0]: https://github.com/stuartsierra/component/tree/component-0.4.0
[0.3.2]: https://github.com/stuartsierra/component/tree/component-0.3.2
[0.3.1]: https://github.com/stuartsierra/component/tree/component-0.3.1
[0.3.0]: https://github.com/stuartsierra/component/tree/component-0.3.0
[0.2.3]: https://github.com/stuartsierra/component/tree/component-0.2.3
[0.2.2]: https://github.com/stuartsierra/component/tree/component-0.2.2
[0.2.1]: https://github.com/stuartsierra/component/tree/component-0.2.1
[0.2.0]: https://github.com/stuartsierra/component/tree/component-0.2.0
[0.1.0]: https://github.com/stuartsierra/component/tree/component-0.1.0

[commit fb891500]: https://github.com/stuartsierra/component/commit/fb891500506b048bd8d9d689dfd3ed8c0e940944
[commit 69e62854]: https://github.com/stuartsierra/component/commit/69e62854c7dac7b4743a542e04ce4aa23a934c07
[commit 7824f551]: https://github.com/stuartsierra/component/commit/7824f55129337c775a776daf6286fd43b8911b38
[commit 5af4ad06]: https://github.com/stuartsierra/component/commit/5af4ad06fdc3ff3240573ae9394da92d8cf90c7e

[#9]: https://github.com/stuartsierra/component/issues/9
[#17]: https://github.com/stuartsierra/component/issues/17
[#40]: https://github.com/stuartsierra/component/issues/40
[#62]: https://github.com/stuartsierra/component/pull/62
[#63]: https://github.com/stuartsierra/component/issues/63
[#65]: https://github.com/stuartsierra/component/issues/65

[dependency]: https://github.com/stuartsierra/dependency
[tools.namespace]: https://github.com/clojure/tools.namespace

[extend-via-metadata]: https://github.com/clojure/clojure/blob/28b87d53909774af28f9f9ba6dfa2d4b94194a57/changes.md#22-protocol-extension-by-metadata
[ClojureScript 1.10.516]: https://github.com/clojure/clojurescript/blob/8a5abc4e02c72d000204674f38c6665c786302a4/changes.md
