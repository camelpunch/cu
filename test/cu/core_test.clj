(ns cu.core-test
  (:require
    [cu.core :as core]
    [expectations :refer :all]
    ))

; copes with empty queues
(expect nil (do
              (core/-main "worker")
              (core/-main "worker")))

; does nothing if given a bad command
(expect nil (core/-main "madeup"))
