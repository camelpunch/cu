(ns cu.payload
  (:require [clojure.set :refer [difference]]))

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

(defn- parse-job [[job-name details]]
  (assoc details :name (name job-name)))

(defn immediate-jobs [pipeline]
  (set (map parse-job (dissoc pipeline :then))))

(defn- flatten-jobs [pipeline]
  (if (contains? pipeline :then)
    (let [with-downstream (conj pipeline (flatten-jobs (pipeline :then)))]
      (dissoc with-downstream :then))
    pipeline))

(defn waiting-jobs [pipeline]
  (difference (set (map parse-job (flatten-jobs pipeline)))
              (immediate-jobs pipeline)))
