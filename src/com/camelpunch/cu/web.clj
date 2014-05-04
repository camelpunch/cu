(ns com.camelpunch.cu.web
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer [not-found]]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn create-app
  []
  (handler/api
   (defroutes app
     (GET "/" []
          {:status 202})
     (not-found "Not Found"))))

(defrecord Web [host port]
  component/Lifecycle

  (start [web-component]
    (println "Starting web server at" (clojure.string/join ":" [host, port]))
    (let [server (run-jetty (create-app)
                            {:host host
                             :port port
                             :join? false})]
      (assoc web-component
        :server server)))

  (stop [web-component]
    (println "Stopping web server")
    (.stop (:server web-component))
    web-component))

(defn web [host port]
  (map->Web {:port port
             :host host}))
