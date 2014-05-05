(ns com.camelpunch.cu.web-server-jetty
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord JettyWebServer [host port application server]
  component/Lifecycle

  (start [web-server-component]
    (println "Starting web server at" (clojure.string/join ":" [host, port]))
    (let [server (run-jetty application
                            {:host host
                             :port port
                             :join? false})]
      (assoc web-server-component
        :server server)))

  (stop [web-server-component]
    (println "Stopping web server")
    (.stop (:server web-server-component))
    web-server-component))

(defn web-server [host port]
  (map->JettyWebServer {:host host
                        :port port}))
