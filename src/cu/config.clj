(ns cu.config
  (:require
    [environ.core :refer [env]]
    ))

(defn- env-or-max [environment k]
  (if-let [env-value (environment k)]
    (Long/parseLong env-value)
    Long/MAX_VALUE))

(defn retrieve-config [environment]
  {:aws-credentials {:access-key (environment :aws-access-key)
                     :secret-key (environment :aws-secret-key)}
   :bucket          (environment :cu-bucket)
   :build-queue     (environment :cu-build-queue)
   :push-queue      (environment :cu-push-queue)
   :cu-username     (environment :cu-username)
   :cu-password     (environment :cu-password)
   :queue-max-wait  (or (environment :cu-max-wait) 500)
   :queue-period    (env-or-max environment :cu-period)
   :workspaces-path (environment :cu-workspaces-path)})

(def config (retrieve-config env))
