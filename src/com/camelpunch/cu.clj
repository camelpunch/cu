(ns com.camelpunch.cu
  (:require [com.stuartsierra.component :as component]
            [com.camelpunch.cu.queue-redis :refer [queue]]
            [com.camelpunch.cu.web :refer [web]]))

(defn build-system []
  (let [queue-host "127.0.0.1"
        queue-port 6379
        web-host "127.0.0.1"
        web-port 3000]
    (component/system-map
     :build-queue (queue "builds" (fn []) queue-host queue-port)
     :push-queue (queue "pushes" (fn []) queue-host queue-port)
     :web (web web-host web-port))))
