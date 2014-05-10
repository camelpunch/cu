(ns com.camelpunch.web-application-test
  (:require [com.camelpunch.cu.web-application :as web]
            [ring.mock.request :as req]
            [clojure.data.json :as json]
            [clojure.core.async :refer [<!! timeout]]
            [clojure.test :refer :all]))

(defn- post [app path request-body]
  ((:ring-handler app)
   (req/body (req/request :post path) request-body)))

(defn- encode-body [body] {:payload (json/write-str body)})

(deftest push-endpoint-returns-202-accepted-response
  (is (= 202
         (:status (post (web/application (timeout 0))
                        "/"
                        (encode-body {}))))))

(deftest push-endpoint-enqueues-given-payload
  (let [push-chan (timeout 50)
        push-payload {"repository" {"name" "foobar"
                                    "url" "/some/url"}}]

    (post (web/application push-chan)
          "/"
          (encode-body push-payload))
    (is (= push-payload (<!! push-chan)))))
