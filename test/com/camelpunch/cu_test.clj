(ns com.camelpunch.cu-test
  (:require [com.camelpunch.cu.queue-redis :refer :all]
            [clojure.test :refer :all]))

(deftest enqueues
  (let [q (new-queue "justatestqueue" (fn []))
        initial-length (length q)
        message-id (enqueue q "justatestitem")]
    (is (= (inc initial-length) (length q)))))
