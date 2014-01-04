(ns cu.core-test
  (:require
    [cu.core :as core]
    [expectations :refer :all]
    ))

; copes with empty queue
(expect nil (do
              (core/-main)
              (core/-main)))
