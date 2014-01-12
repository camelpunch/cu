(ns cu.runner
  (:require
    [clojure.java.shell :refer [sh]]))

(defn run!
  "Run a script. Currently only supports single commands without arguments."
  [working-directory script]
  (sh script :dir working-directory))

(defn decide-whether-to [f g]
  "Takes two single-argument functions with side-effects, f and g.
  Returns single-argument function that takes a.
  That function returns f of a if g of a is truthy, or nil."
  (fn [a] (if (g a) (f a))))
