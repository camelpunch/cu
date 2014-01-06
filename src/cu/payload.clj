(ns cu.payload)

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

(defn- job-from-job-config [[job-name details]]
  (assoc details :name (name job-name)))

(defn immediate-jobs [cu-config]
  (map job-from-job-config (dissoc (cu-config :pipeline) :then)))

