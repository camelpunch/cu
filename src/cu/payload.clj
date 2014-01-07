(ns cu.payload)

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

(defn- parse-job [[job-name details]]
  (assoc details :name (name job-name)))

(defn- flatten-jobs [pipeline]
  (if-let [downstream (pipeline :then)]
    (dissoc (conj pipeline (flatten-jobs downstream))
            :then)
    pipeline))

(defn immediate-jobs [pipeline]
  (map parse-job (dissoc pipeline :then)))

(defn waiting-jobs [pipeline]
  (reverse (map parse-job (flatten-jobs (pipeline :then)))))
