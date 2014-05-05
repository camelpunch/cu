(ns com.camelpunch.cu.web-application
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer [not-found]]))

(defroutes app
  (POST "/" []
        {:status 202})
  (not-found "Not Found"))

(defn application []
  {:ring-handler (handler/api app)})
