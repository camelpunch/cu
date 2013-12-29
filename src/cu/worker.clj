(ns cu.worker
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.java.shell :refer [sh]]
    [cu.git :as git]
    [environ.core :refer [env]]
    ))

(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))
(def sqs-client (sqs/create-client (env :aws-access-key)
                                   (env :aws-secret-key)))
(def q (sqs/create-queue sqs-client (env :cu-queue-name)))

(defn run
  [queue-name]
  (let [workspaces-dir (env :workspaces-path)
        basedir (str workspaces-dir "/test-project")
        workspace-dir (str basedir "/workspace")
        payload (->> (sqs/receive sqs-client q)
                     first
                     :body
                     read-string)]
    (.mkdirs (java.io.File. basedir))
    (git/fresh-clone (get-in payload ["repository" "url"]) workspace-dir)
    (s3/put-object aws-creds bucket log-key
                   (:out (sh (str workspace-dir "/run-pipeline"))))))
