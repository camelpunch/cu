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

(defn- path [& parts] (join "/" parts))

(defn- workspace-dir [basedir url]
  (path basedir (last (split url #"/")) "workspace"))

(defn process-push-message [client workspaces-path build-queue-name {payload :body}]
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
                   "to build queue:" job)
          (sqs/send client
                    (sqs-queue client build-queue-name)
                    (pr-str job)))))))

(defn- read-body-keys [message & ks]
  (-> (message :body)
      read-string
      (select-keys ks)))

(defn message-job-runner
  "Returns a fn that processes a message from the worker queue.
  Runs script, stores output.
  Currently outputs to both a big log and separate job logs.
  TODO: Remove big log."
  [{:keys [workspaces-path log-key
           aws-credentials bucket]}]
  (println "--------------------------------------")
  (println "message-job-runner")
  (fn [message]
    (let [job                 (read-body-keys message :uuid :name :repo :ref :script)

          workspace-path      (path workspaces-path (job :name) "workspace")

          existing-big-log    (io/get-key aws-credentials bucket log-key)

          repo                (git/fresh-clone (job :repo) workspace-path (job :ref))
          build               (runner/run! workspace-path (job :script))
          ]

      (println "for" (job :name))
      (io/put aws-credentials bucket
              (results/log-key (job :uuid) (job :name) (build :exit))
              (build :out))
      (io/put aws-credentials bucket
              log-key
              (str existing-big-log (build :out)))
      (build :out))))

(defn- no-pending-upstream-jobs?
  "Predicate used to filter out messages to leave in queue
  i.e. don't process and don't delete."
  [credentials bucket
   message]
  (let [ls                    (partial io/ls
                                       credentials
                                       bucket)
        job                   (read-body-keys message :name :uuid :upstream)
        all-results           (ls (job :uuid))
        completed-job-names   (results/job-names all-results)]
    (println "--------------------------------------")
    (println "no-pending-upstream-jobs? for" (job :name))
    (println "raw list:" all-results)
    (println "superset?" completed-job-names (job :upstream))
    (doto (superset? completed-job-names (job :upstream)) println)))

(defn parser
  "Grabs notifications from the push-queue,
  clones each git repo pushed
  and enqueues on the build queue based on each repo's pipeline config."
  [config]
  (let [client (sqs-client (vals (config :aws-credentials)))
        q (sqs-queue client (config :push-queue))]
    (println "Starting parser")
    (dorun
      (map (sqs/deleting-consumer client
                                  (partial process-push-message
                                           client
                                           (config :workspaces-path)
                                           (config :build-queue)))
           (sqs/polling-receive client q
                                :max-wait (config :queue-max-wait)
                                :period (config :queue-period)
                                :limit 10)))))

(defn worker [config]
  (let [credentials           (config :aws-credentials)
        client                (sqs-client (vals credentials))
        q                     (sqs-queue client (config :build-queue))
        ls                    (partial io/ls credentials (config :bucket))
        run-job-from-message  (message-job-runner config)
        upstream-all-passed?  (fn [message]
                                (let [job                (read-body-keys message :name :uuid :upstream)
                                      passed-job-names   (results/passed-job-names (ls (job :uuid)))]
                                  (println "--------------------------------------")
                                  (println "upstream-all-passed? for" (job :name))
                                  (doto (or (empty? (job :upstream))
                                            (superset? passed-job-names (job :upstream))) println)))]
    (println "Starting worker")
    (dorun
      (map (sqs/deleting-consumer client
                                  (runner/decide-whether-to
                                    run-job-from-message
                                    upstream-all-passed?))
           (filter (partial no-pending-upstream-jobs?
                            (config :aws-credentials)
                            (config :bucket))
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
