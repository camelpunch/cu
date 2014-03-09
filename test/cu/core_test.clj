(ns cu.core-test
  (:require
    [cu.core :as core]
    [expectations :refer :all]
    ))

(expect nil (core/-main "bad command"))
