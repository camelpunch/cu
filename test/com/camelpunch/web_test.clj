(ns com.camelpunch.web-test
  (:require [com.camelpunch.cu.web :as web]
            [ring.mock.request :refer [request body header]]
            [clojure.test :refer :all]))

(deftest accepted-response-to-push-notification
  (is (= 202 (:status ((web/create-app) (request :get "/"))))))
