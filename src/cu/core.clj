(ns cu.core
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.java.shell :refer [sh]]
    [cu.git :as git]
    [environ.core :refer [env]]
    )
  (:gen-class))

(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))

(defn- sqs-client [] (sqs/create-client (env :aws-access-key)
                                       (env :aws-secret-key)))
(defn- sqs-queue [client queue-name] (sqs/create-queue client queue-name))

(defn run
  [queue-name]
  (let [workspaces-dir (env :workspaces-path)
        basedir (str workspaces-dir "/test-project")
        workspace-dir (str basedir "/workspace")
        client (sqs-client)
        q (sqs-queue client queue-name)
        raw-payload (->> (sqs/receive client q)
                         first
                         :body)
        payload (if raw-payload
                  (read-string raw-payload)
                  {})]
    (when-let [url (get-in payload ["repository" "url"])]
      (.mkdirs (java.io.File. basedir))
      (git/fresh-clone url workspace-dir)
      (s3/put-object aws-creds bucket log-key
                     (:out (sh (str workspace-dir "/run-pipeline"))))
      "Processed payload for URL" url)))

(defn -main [& [queue-name]]
  (loop []
    (println (run queue-name))
    (Thread/sleep 2000)
    (recur)))
