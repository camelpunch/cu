(ns com.camelpunch.cu.queue-redis
  (:require
   [com.stuartsierra.component :as component]
   [taoensso.carmine.message-queue :as car-mq]))

(defrecord Queue [pool spec queue-name worker]
  component/Lifecycle

  (start [component]
    (let [conn {:pool pool :spec spec}
          worker (car-mq/worker conn queue-name)]
      (assoc component :worker worker)))

  (stop [component]
    (car-mq/stop worker)))

(defn new-queue [queue-name]
  (map->Queue {:pool {}
               :spec {}
               :queue-name queue-name}))
