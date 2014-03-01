(ns cu.io
  (:require
    [aws.sdk.s3 :as s3]))

(defn put [credentials bucket k v]
  (s3/put-object credentials bucket k v))

(defn get-key
  "Gets a key and returns nil if not present."
  [credentials bucket k]
  (try
    (-> (s3/get-object credentials bucket k)
        :content
        slurp)
    (catch
      com.amazonaws.services.s3.model.AmazonS3Exception e nil)))

(defn delete [credentials bucket k]
  (s3/delete-object credentials bucket k))

(defn ls [credentials bucket prefix]
  (map :key (:objects (s3/list-objects credentials bucket {:prefix prefix}))))

