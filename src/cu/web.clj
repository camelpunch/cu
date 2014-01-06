(ns cu.web
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.data.json :as json]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :refer [not-found]]
    [cu.config :refer [config]]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.basic-authentication :refer :all]
    ))

(defn- sqs-client [] (apply sqs/create-client
                            (vals (config :aws-credentials))))
(defn- sqs-queue [client] (sqs/create-queue client "cu-pushes"))

(defn- authenticated? [username password]
  (and (= username (config :cu-username))
       (= password (config :cu-password))))

(defroutes app-routes
  (POST "/push" [_ & {raw-payload :payload}]
        (let [client (sqs-client)]
          (sqs/send client
                    (sqs-queue client)
                    (pr-str (json/read-str raw-payload)))
          {:status 202}))

  (GET "/logs" []
       {:status 200
        :body (-> (apply s3/get-object
                         (mapv config [:aws-credentials :bucket :log-key]))
                  :content
                  slurp)})

  (not-found "Not Found"))

(def app (-> (handler/api app-routes)
             (wrap-basic-authentication authenticated?)))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app {:port port})))

