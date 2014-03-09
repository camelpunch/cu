(ns cu.results
  (:require
    [clojure.string :refer [join]]))

(defn log-key [uuid job-name exit-code]
  (join "/" ["logs" uuid (str job-name "-exit-" exit-code)]))

(defn- job-name [log-key]
  (clojure.string/replace log-key #"logs/[^/]+/(.+)-exit-\d+$" "$1"))

(defn job-names [log-keys]
  (set (map job-name log-keys)))

(defn- passed-job-name? [log-key]
  (re-find #"-exit-0$" log-key))

(defn passed-job-names [log-keys]
  (job-names (filter passed-job-name? log-keys)))
