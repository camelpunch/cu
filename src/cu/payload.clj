(ns cu.payload)

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

; what follows really belongs in the config namespace
(defn- parse-job [[job-name details]]
  (assoc details :name (name job-name)))

(defn- flatten-jobs [{downstream :downstream :as pipeline}]
  (if downstream
    (dissoc (conj pipeline (flatten-jobs downstream))
            :downstream)
    pipeline))

(defn immediate-jobs [pipeline]
  (map parse-job (dissoc pipeline :downstream)))

(defn waiting-jobs [{downstream :downstream :or {}}]
  (reverse (map parse-job (flatten-jobs downstream))))

