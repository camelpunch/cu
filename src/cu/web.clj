(ns cu.web
  (:require [compojure.core :refer :all]
            [compojure.route :refer [not-found]]
            [compojure.handler :refer [site]]
            [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defroutes app-routes
  (POST "/push" [_ & {raw-payload :payload}]
        (let [basedir "/tmp/cu-workspaces/test-project"
              workspace-dir (str basedir "/workspace")
              payload (json/read-str raw-payload)]
          (.mkdirs (java.io.File. basedir))
          (sh "rm" "-r" workspace-dir)
          (sh "git" "clone" (get-in payload ["repository" "url"]) workspace-dir)
          (spit "/tmp/cu-workspaces/test-project/log"
                (:out (sh (str workspace-dir "/run-pipeline"))))
          {:status 201}))

  (not-found "Not Found"))

(def app (site app-routes))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app {:port port})))

