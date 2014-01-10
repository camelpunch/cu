(ns cu.core
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clj-time.local :refer [local-now]]
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

(defn process-push-message [client {payload :body}]
  (when-let [url (payload/clone-target-url payload)]
    (let [repo (git/fresh-clone url (workspace-dir (config :workspaces-path) url))]
      (doseq [job (payload/immediate-jobs (-> repo :config :pipeline))]
        (sqs/send client
                  (sqs-queue client "cu-immediate")
                  (pr-str job)))
      (doseq [job (payload/waiting-jobs (-> repo :config :pipeline))]
        (sqs/send client
                  (sqs-queue client "cu-waiting")
                  (pr-str job))))))

; currently only supports single commands
(defn- run! [working-directory script]
  (sh script :dir working-directory))

(defn process-job-message [{raw-job :body}]
  (let [
        {job-name :name
         script   :script
         url      :repo} (read-string raw-job)

        workspace (join "/" [(config :workspaces-path) job-name "workspace"])
        repo (git/fresh-clone url workspace)
        build (run! workspace script)
        [date timestamp] (split (str (local-now)) #"T")
        ]
    (s3/put-object
      (config :aws-credentials)
      (config :bucket)
      (join "/"
            [job-name
             "builds"
             date
             (join "_" [timestamp (repo :sha) (build :exit)])])
      (build :out))
    (str
      "Processed payload for URL " url " with output " (build :out))))

(defn -main [command & args]
  (case command

    parser
    (let [client (sqs-client)
          q (sqs-queue client "cu-pushes")]
      (dorun
        (map (sqs/deleting-consumer client (partial process-push-message client))
             (sqs/polling-receive client q
                                  :max-wait (config :cu-max-wait)
                                  :period (config :cu-period)
                                  :limit 10))))

    shoveler
    (println "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAshoveling")
    ; (fn [message]
    ;            (let [ret (process-job-message message)]
    ;              (sqs/delete client message)
    ;              ret))

    worker
    (let [client (sqs-client)
          q (sqs-queue client "cu-immediate")]
      (dorun
        (map (sqs/deleting-consumer client process-job-message)
             (sqs/polling-receive client q
                                  :max-wait (config :cu-max-wait)
                                  :period (config :cu-period)
                                  :limit 10))))

    nil))
