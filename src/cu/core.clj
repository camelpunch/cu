(ns cu.core
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [split join]]
    [cu.config :refer [config]]
    [cu.git :as git]
    [cu.payload :as payload]
    )
  (:gen-class))

(defn- sqs-client [] (apply sqs/create-client
                            (vals (config :aws-credentials))))
(defn- sqs-queue [client queue-name] (sqs/create-queue client queue-name))

(defn process-message [message]
  (let [raw-payload (:body message)]
    (when-let [url (payload/clone-target-url raw-payload)]
      (let [workspace-dir (join "/" [(config :workspaces-path)
                                     (last (split url #"/"))
                                     "workspace"])]
        (git/fresh-clone url workspace-dir)
        (let [output (-> (str workspace-dir "/run-pipeline") sh :out)]
          (apply s3/put-object (conj (mapv config [:aws-credentials
                                                   :bucket
                                                   :log-key])
                                     output))
          (str
            "Processed payload for URL " url " with output " output))))))

(defn -main []
  (let [client (sqs-client)
        q (sqs-queue client (config :queue))]
    (dorun (map (sqs/deleting-consumer client (comp println process-message))
                (sqs/polling-receive client q
                                     :max-wait (config :cu-max-wait)
                                     :period (config :cu-period)
                                     :limit 10)))))
