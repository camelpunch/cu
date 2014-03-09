(ns cu.web
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.data.json :as json]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :refer [not-found]]
    [cu.config :as global-config]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.basic-authentication :refer :all]
    ))

(defn- sqs-client [credentials] (apply sqs/create-client (vals credentials)))
(defn- sqs-queue [client queue-name] (sqs/create-queue client queue-name))

(defn- authenticator [username password]
  (fn [request-username request-password]
    (and (= request-username username)
         (= request-password password))))

(defn create-app-routes [config]
  (defroutes app-routes
    (POST "/push" [_ & {raw-payload :payload}]
          (let [client (sqs-client (config :aws-credentials))]
            (sqs/send client
                      (sqs-queue client (config :push-queue))
                      (pr-str (json/read-str raw-payload)))
            {:status 202}))

    (GET "/logs" []
         {:status 200
          ; TODO: list objects - get latest push dir,
          ; then concat the logs in that dir
          :body (-> (apply s3/get-object
                           (mapv config [:aws-credentials :bucket :log-key]))
                    :content
                    slurp)})

    (DELETE "/logs" []
            (apply s3/delete-object
                   (mapv config [:aws-credentials :bucket :log-key]))
            {:status 200})

    (not-found "Not Found")))

(defn app [config]
  (-> (handler/api (create-app-routes config))
      (wrap-basic-authentication (authenticator (config :cu-username)
                                                (config :cu-password)))))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty (app global-config/config) {:port port})))

