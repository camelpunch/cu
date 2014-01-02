(ns cu.config
  (:require
    [clj-yaml.core :as yaml]
    [environ.core :refer [env]]
    ))

(defn- env-or-max [environment k]
  (if-let [env-value (environment k)]
    (Long/parseLong env-value)
    Long/MAX_VALUE))

(defn- retrieve-config [environment]
  (-> (str (environment :home) "/cu_worker.yml")
      slurp
      yaml/parse-string
      (assoc :aws-credentials {:access-key (environment :aws-access-key)
                               :secret-key (environment :aws-secret-key)}
             :cu-username (env :cu-username)
             :cu-password (env :cu-password)
             :cu-max-wait (env-or-max environment :cu-max-wait)
             :cu-period (env-or-max environment :cu-period))))
(def retrieve-config-memo (memoize retrieve-config))

(defn config [k] ((retrieve-config-memo env) k))
