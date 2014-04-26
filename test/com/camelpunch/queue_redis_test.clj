(ns com.camelpunch.queue-redis-test
  (:require [com.camelpunch.cu.queue-redis :refer :all]
            [clojure.test :refer :all]))

(deftest enqueues
  (let [q (new-queue "justatestqueue" (fn []) "127.0.0.1" 6379)
        initial-length (length q)
        message-id (enqueue q "justatestitem")]
    (is (= (inc initial-length) (length q)))))
