(ns cu.runner-test
  (:require
    [expectations :refer :all]
    [cu.runner :refer :all]))

(expect
  :foo
  (let [f identity
        g (fn [a] (= :foo a))
        h (decide-whether-to f g)]
    (h :foo)))

(expect
  nil
  (let [f (fn [_] (assert false))
        g (fn [a] (= :bar a))
        h (decide-whether-to f g)]
    (h :baz)))
