(ns cu.core-test
  (:require
    [cu.core :as worker]
    [environ.core :refer [env]]
    [expectations :refer :all]
    ))

; returns nil with empty queue
(expect nil (worker/run "emptyqueue"))
