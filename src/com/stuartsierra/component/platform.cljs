(ns com.stuartsierra.component.platform
  "Platform-specific implementation details for Component on
  ClojureScript (JavaScript). This is not a public API.")

(defn argument-error [message]
  (ex-info message {:reason ::illegal-argument}))

(defn type-name
  "Returns a string name for the type/class of x."
  [x]
  (type->str (type x)))

(defn alter-ex-data
  "Returns a new ExceptionInfo with the same details as error and
  ex-data as the result of (apply f (ex-data throwable) args)."
  [error f & args]
  (let [ex (ex-info (ex-message error)
                    (apply f (ex-data error) args)
                    (ex-cause error))]
    ;; Set same properties as cljs.core/ex-info:
    (set! (.-name ex) (.-name error))
    (set! (.-description ex) (.-description error))
    (set! (.-number ex) (.-number error))
    (set! (.-fileName ex) (.-fileName error))
    (set! (.-lineNumber ex) (.-lineNumber error))
    (set! (.-columnNumber ex) (.-columnNumber error))
    (set! (.-stack ex) (.-stack error))
    ex))

;; Copyright © 2015 Stuart Sierra

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
