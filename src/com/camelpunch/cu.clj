(ns com.camelpunch.cu
  (:require [com.stuartsierra.component :as component]
            [com.camelpunch.cu.queue-redis :as queue]))

(defn build-system []
  (let [host "127.0.0.1"
        port 6379]
    (component/system-map
     :build-queue (queue/new-queue "builds" (fn []) host port)
     :push-queue (queue/new-queue "pushes" (fn []) host port))))
