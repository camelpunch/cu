(ns com.camelpunch.cu.web-application
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer [not-found]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [go >!]]))

(defn application [push-chan]
  (let [app-routes (routes
                    (POST "/" [_ & {raw-payload :payload}]
                          (go
                           (>! push-chan (json/read-str raw-payload)))
                          {:status 202})
                    (not-found "Not Found"))]
    {:ring-handler (handler/api app-routes)}))
