(ns cu.results
  (:require
    [clojure.string :refer [split]]))

(defn log-key [uuid job-name exit-code]
  (str uuid "/" job-name "-exit-" exit-code))

(defn- job-name [log-key]
  (second (split (first (split log-key #"-exit-"))
                 #"/" 2)))

(defn job-names [log-keys]
  (set (map job-name log-keys)))

(defn- passed-job-name? [log-key]
  (re-find #"-exit-0$" log-key))

(defn passed-job-names [log-keys]
  (job-names (filter passed-job-name? log-keys)))
