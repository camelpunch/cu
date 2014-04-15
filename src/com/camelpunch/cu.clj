(ns com.camelpunch.cu
  (:require [com.stuartsierra.component :as component]
            [cemerick.bandalore :as sqs]))

(defrecord QueueClient [access-key secret-key]
  component/Lifecycle

  (start [component]
    (println ";; Creating SQS client")
    (let [client (sqs/create-client access-key secret-key)]
      (assoc component :client client)))

  (stop [component]
    (println ";; Faking shutdown of SQS client")))

(defn build-system [config-options]
  (let [{:keys [aws-access-key aws-secret-key]} config-options]
    (component/system-map
     :queue (QueueClient. aws-access-key aws-secret-key))))

