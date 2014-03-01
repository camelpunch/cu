(ns cu.config
  (:require
    [clj-yaml.core :as yaml]
    [environ.core :refer [env]]
    ))

(defn- env-or-max [environment k]
  (if-let [env-value (environment k)]
    (Long/parseLong env-value)
    Long/MAX_VALUE))

(defn retrieve-config [environment]
  (-> (str (environment :home) "/cu.yml")
      slurp
      yaml/parse-string
      (assoc :aws-credentials {:access-key (environment :aws-access-key)
                               :secret-key (environment :aws-secret-key)}
             :cu-username (environment :cu-username)
             :cu-password (environment :cu-password)
             :queue-max-wait (env-or-max environment :cu-max-wait)
             :queue-period (env-or-max environment :cu-period)
             :workspaces-path (environment :cu-workspaces-path))))

(def config (retrieve-config env))
