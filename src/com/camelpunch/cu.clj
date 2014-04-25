(ns com.camelpunch.cu
  (:require [com.stuartsierra.component :as component]
            [com.camelpunch.cu.queue-redis :as queue]))

(defn build-system []
  (component/system-map
   :build-queue (queue/new-queue "builds" (fn []))
   :push-queue (queue/new-queue "pushes" (fn []))))
