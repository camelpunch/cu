(ns cu.core-test
  (:require
    [cu.core :as core]
    [environ.core :refer [env]]
    [expectations :refer :all]
    ))

; returns nil with empty queue
(expect nil (core/-main "emptyqueue"))
