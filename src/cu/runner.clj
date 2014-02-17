(ns cu.runner
  (:require
    [clojure.java.shell :refer [sh]]))

(defn run!
  "Run a script. Currently only supports single commands without arguments."
  [working-directory script]
  (sh script :dir working-directory))

(defn decide-whether-to [f g]
  (fn [a] (if (g a) (f a))))
