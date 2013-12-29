(ns cu.web
  (:require
    [aws.sdk.s3 :as s3]
    [cemerick.bandalore :as sqs]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :refer [not-found]]
    [cu.git :as git]
    [cu.worker :as worker]
    [environ.core :refer [env]]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.basic-authentication :refer :all]
    ))

(def sqs-client (sqs/create-client (env :aws-access-key)
                                   (env :aws-secret-key)))
(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))

(def q (sqs/create-queue sqs-client (env :cu-queue-name)))

(defn authenticated? [username password]
  (and (= username (env :cu-username))
       (= password (env :cu-password))))

(defroutes app-routes
  (POST "/push" [_ & {raw-payload :payload}]
        (sqs/send sqs-client q (pr-str (json/read-str raw-payload)))
        (worker/run (env :cu-queue-name))
        {:status 201})
  (GET "/logs" []
       {:status 200
        :body (slurp (:content (s3/get-object aws-creds bucket log-key)))})

  (not-found "Not Found"))

(def app (-> (handler/api app-routes)
             (wrap-basic-authentication authenticated?)))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app {:port port})))

