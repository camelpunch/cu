(ns cu.web
  (:require
    [aws.sdk.s3 :as s3]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [compojure.core :refer :all]
    [compojure.handler :refer [site]]
    [compojure.route :refer [not-found]]
    [environ.core :refer [env]]
    [ring.adapter.jetty :refer [run-jetty]]
    [cu.git :as git]
    ))

(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))

(defroutes app-routes
  (POST "/push" [_ & {raw-payload :payload}]
        (let [workspaces-dir (env :workspaces-path)
              basedir (str workspaces-dir "/test-project")
              workspace-dir (str basedir "/workspace")
              payload (json/read-str raw-payload)]
          (.mkdirs (java.io.File. basedir))
          (git/fresh-clone (get-in payload ["repository" "url"]) workspace-dir)
          (s3/put-object aws-creds bucket log-key
                         (:out (sh (str workspace-dir "/run-pipeline"))))
          {:status 201}))

  (not-found "Not Found"))

(def app (site app-routes))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app {:port port})))

