(ns com.camelpunch.cu.queue-redis
  (:require
   [com.stuartsierra.component :as component]
   [taoensso.carmine :as car :refer [wcar]]
   [taoensso.carmine.message-queue :as car-mq]))

(defrecord RedisQueue [pool spec name handler worker]
  component/Lifecycle

  (start [component]
    (let [conn {:pool pool :spec spec}
          worker (car-mq/worker conn name
                                {:handler handler})]
      (assoc component
        :worker worker
        :connection conn)))

  (stop [component]
    (car-mq/stop worker)))

(defn new-queue [name handler]
  (map->RedisQueue {:pool {}
                    :spec {:host "127.0.0.1"
                           :port 6379}
                    :name name
                    :handler handler}))

(defn enqueue [queue item]
  (car/wcar (:connection queue) (car-mq/enqueue (:name queue) item)))

(defn length [queue]
  (count (:messages
          (car-mq/queue-status (:connection queue) (:name queue)))))
