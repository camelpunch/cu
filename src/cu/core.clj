(ns cu.core
  (:import java.util.UUID)
  (:require
    [cemerick.bandalore :as sqs]
    [clojure.set :refer [superset?]]
    [clojure.string :refer [split join]]
    [cu.config :as global-config]
    [cu.git :as git]
    [cu.io :as io]
    [cu.payload :as payload]
    [cu.results :as results]
    [cu.runner :as runner]
    )
  (:gen-class))

(defn- sqs-client [credentials] (apply sqs/create-client credentials))
(defn- sqs-queue [client queue-name] (sqs/create-queue client queue-name))

(defn- workspace-dir [basedir url]
  (join "/" [basedir (last (split url #"/")) "workspace"]))

(defn process-push-message [client workspaces-path {payload :body}]
  (when-let [url (payload/clone-target-url payload)]
    (let [repo (git/fresh-clone url
                                (workspace-dir workspaces-path url)
                                "master")
          uuid (str (java.util.UUID/randomUUID))]
      (println "Iterating over jobs for" url)
      (doseq [partial-job (payload/all-jobs (-> repo :config :pipeline))]
        (let [job (assoc partial-job
                         :uuid  uuid
                         :ref   (repo :ref))]
          (println "--------------------------------------")
          (println "Pushing build of" (partial-job :name)
                   "to cu-builds:" job)
          (sqs/send client
                    (sqs-queue client "cu-builds")
                    (pr-str job)))))))

(defn- read-body [message]
  (read-string (message :body)))

(defn run-job-from-message
  "Process a queue item from the worker queue. Runs script, stores output.
  Currently outputs to both a big log and separate job logs.
  TODO: Remove big log."
  [workspaces-path big-log-key message]
  (println "--------------------------------------")
  (println "run-job-from-message")
  (let [{uuid     :uuid
         job-name :name
         git-url  :repo
         git-ref  :ref
         script   :script}  (read-body message)

        workspace-path      (join "/" [workspaces-path job-name "workspace"])

        existing-big-log    (io/get-key big-log-key)

        repo                (git/fresh-clone git-url workspace-path git-ref)
        build               (runner/run! workspace-path script)
        ]

    (println "for" job-name)
    (io/put (results/log-key uuid job-name (build :exit))
            (build :out))
    (io/put big-log-key (str existing-big-log (build :out)))
    (build :out)))

(defn- upstream-all-passed?
  "Truthy if there are either no upstream jobs for the message, or
  all upstream jobs have a key-value entry with zero exit code."
  [message]
  (let [{job-name             :name
         uuid                 :uuid
         upstream-job-names   :upstream} (read-body message)
        passed-job-names      (results/passed-job-names (io/ls uuid))]
    (println "--------------------------------------")
    (println "upstream-all-passed? for" job-name)
    (doto (or (empty? upstream-job-names)
              (superset? passed-job-names upstream-job-names)) println)))

(defn- no-pending-upstream-jobs?
  "Predicate used to filter out messages to leave in queue
  i.e. don't process and don't delete."
  [message]
  (let [{job-name             :name
         uuid                 :uuid
         upstream-job-names   :upstream} (read-body message)
        all-results           (io/ls uuid)
        completed-job-names   (results/job-names all-results)]
    (println "--------------------------------------")
    (println "no-pending-upstream-jobs? for" job-name)
    (println "raw list:" all-results)
    (println "superset?" completed-job-names upstream-job-names)
    (doto (superset? completed-job-names upstream-job-names) println)))

(defn parser [config]
  (let [client (sqs-client (vals (config :aws-credentials)))
        q (sqs-queue client "cu-pushes")] ; TODO: use queue name from config
    (println "Starting parser")
    (dorun
      (map (sqs/deleting-consumer client
                                  (partial process-push-message
                                           client
                                           (config :workspaces-path)))
           (sqs/polling-receive client q
                                :max-wait (config :queue-max-wait)
                                :period (config :queue-period)
                                :limit 10)))))

(defn worker [config]
  (let [client (sqs-client (vals (config :aws-credentials)))
        q (sqs-queue client "cu-builds")]
    (println "Starting worker")
    (dorun
      (map (sqs/deleting-consumer client
                                  (runner/decide-whether-to
                                    (partial run-job-from-message
                                             (config :workspaces-path)
                                             (config :log-key))
                                    upstream-all-passed?))
           (filter no-pending-upstream-jobs?
                   (sqs/polling-receive client q
                                        :max-wait (config :queue-max-wait)
                                        :period (config :queue-period)
                                        :limit 10))))))

(defn -main [command & args]
  (case command

    "parser"
    (parser global-config/config)

    "worker"
    (worker global-config/config)

    nil))
