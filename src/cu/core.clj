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

(defn- workspace-dir [basedir url]
  (join "/" [basedir (last (split url #"/")) "workspace"]))
(comment (let [url "http://some.url/pathname"]
           (workspace-dir (config :workspaces-path) url)))

(defn process-message [message]
  (when-let [url (payload/clone-target-url (message :body))]
    (let [repo (git/fresh-clone url
                                (workspace-dir (config :workspaces-path) url))
          output (apply str
                        (map (fn [script] (-> script sh :out))
                             (repo :scripts)))]
      (apply s3/put-object (conj (mapv config [:aws-credentials
                                               :bucket
                                               :log-key])
                                 output))
      (str
        "Processed payload for URL " url " with output " output))))

(defn -main []
  (let [client (sqs-client)
        q (sqs-queue client (config :queue))]
    (dorun
      (map (sqs/deleting-consumer client
                                  (fn [message]
                                    ; (try
                                    (process-message message)
                                    ; (catch Exception e (println e)))
                                    ))
           (sqs/polling-receive client q
                                :max-wait (config :cu-max-wait)
                                :period (config :cu-period)
                                :limit 10)))))
