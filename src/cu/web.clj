(ns cu.web
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clj-yaml.core :as yaml]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :refer [not-found]]
    [environ.core :refer [env]]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.basic-authentication :refer :all]
    ))

(defn- sqs-client [] (sqs/create-client (env :aws-access-key)
                                        (env :aws-secret-key)))
(def config (-> (str (env :home) "/cu_worker.yml")
                slurp
                yaml/parse-string))
(defn- sqs-queue [client] (sqs/create-queue client (config :queue)))

(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))

(defn- authenticated? [username password]
  (and (= username (env :cu-username))
       (= password (env :cu-password))))

(defroutes app-routes
  (POST "/push" [_ & {raw-payload :payload}]
        (let [client (sqs-client)
              q (sqs-queue client)]
          (sqs/send client q (pr-str (json/read-str raw-payload)))
          {:status 202}))
  (GET "/logs" []
       {:status 200
        :body (slurp (:content (s3/get-object aws-creds bucket log-key)))})

  (not-found "Not Found"))

(def app (-> (handler/api app-routes)
             (wrap-basic-authentication authenticated?)))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app {:port port})))

