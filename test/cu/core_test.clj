(ns cu.core-test
  (:require
    [cu.core :as core]
    [expectations :refer :all]
    ))

(expect nil (do
              (core/-main)
              (core/-main)))
