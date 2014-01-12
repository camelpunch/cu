(ns cu.io
  (:require
    [cu.config :refer [config]]
    [aws.sdk.s3 :as s3]))

(defn put [k v]
  (s3/put-object (config :aws-credentials)
                 (config :bucket)
                 k v))

(defn get-key
  "Gets a key and returns nil if not present."
  [k]
  (try
    (-> (s3/get-object (config :aws-credentials)
                       (config :bucket)
                       k)
        :content
        slurp)
    (catch
      com.amazonaws.services.s3.model.AmazonS3Exception e nil)))

(defn delete [k]
  (s3/delete-object (config :aws-credentials)
                    (config :bucket)
                    k))

(defn ls [prefix]
  (map :key (:objects (s3/list-objects (config :aws-credentials)
                                       (config :bucket)
                                       {:prefix prefix}))))

