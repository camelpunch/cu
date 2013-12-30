(ns cu.core
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.string :refer [split join]]
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

(defn- clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

(defn- workspace-dir [workspaces-dir url]
  (join "/" [workspaces-dir
             (last (split url #"/"))
             "workspace"]))
(comment
  (workspace-dir "spaces" "http://github.bob/foobar/projectx"))

(defn run [queue-name]
  (let [client (sqs-client)
        q (sqs-queue client queue-name)
        raw-payload (-> (sqs/receive client q)
                        first
                        :body)]
    (when-let [url (clone-target-url raw-payload)]
      (let [ws-dir (workspace-dir (env :workspaces-path) url)]
        (git/fresh-clone url ws-dir)
        (let [output (:out (sh (str ws-dir "/run-pipeline")))]
          (s3/put-object aws-creds bucket log-key output)
          (str
            "Processed payload for URL " url " with output " output))))))

(defn -main [& [queue-name]]
  (loop []
    (println (run queue-name))
    (Thread/sleep 2000)
    (recur)))
