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

(defn- upstream-job-names [job-name rest-of-pipeline & [previous-level]]
  (cond
    (rest-of-pipeline job-name)     (->> (keys (or previous-level rest-of-pipeline))
                                         (remove #{:downstream})
                                         (map name)
                                         set)
    (rest-of-pipeline :downstream)  (upstream-job-names job-name
                                                        (rest-of-pipeline :downstream)
                                                        rest-of-pipeline)
    :else                           #{}))

(defn- parse-job [[job-name details] & [pipeline]]
  (assoc details
         :name (name job-name)
         :upstream (upstream-job-names job-name (or pipeline {}))))

(defn- flatten-jobs [{downstream :downstream :as rest-of-pipeline}]
  (if downstream
    (dissoc (conj rest-of-pipeline (flatten-jobs downstream))
            :downstream)
    rest-of-pipeline))

(defn immediate-jobs [pipeline]
  (map parse-job (dissoc pipeline :downstream)))

(defn waiting-jobs [{first-downstream :downstream :or {} :as whole-pipeline}]
  (reverse (map #(parse-job % whole-pipeline) (flatten-jobs first-downstream))))

(defn all-jobs [pipeline]
  (concat (immediate-jobs pipeline) (waiting-jobs pipeline)))

