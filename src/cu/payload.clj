(ns cu.payload)

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

(defn next-job-action
  "Takes a message from a queue and a function for fetching the currently
  passed upstream jobs for a given job. Returns an action that should be
  performed next."
  [{payload :body} fetch-passed-jobs]
  (let [job (read-string payload)]
    (if (empty? (job :upstream))
      :run
      (let [passed-jobs (fetch-passed-jobs job)]
        (cond
          (nil? passed-jobs)              :wait
          (= passed-jobs (job :upstream)) :run
          :else                           :delete)))))


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

