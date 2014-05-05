(ns com.camelpunch.web-application-test
  (:require [com.camelpunch.cu.web-application :as web]
            [ring.mock.request :refer [request body header]]
            [clojure.data.json :as json]
            [clojure.test :refer :all]))

(deftest push-notification-responds-with-202-accepted
  (let [payload {:repository {:name "test-project"
                              :url "/some/path/to/a/git/repo.git"}}
        json-payload (json/write-str payload)]
    (is (= 202 (:status ((:ring-handler (web/application))
                         (body (request :post "/")
                               {:payload json-payload})))))))
