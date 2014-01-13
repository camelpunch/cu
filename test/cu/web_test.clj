(ns cu.web-test
  (:import java.util.UUID)
  (:require
    [cemerick.bandalore :as sqs]
    [clj-yaml.core :as yaml]
    [clojure.data.codec.base64 :as b64]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [join trim-newline]]
    [cu.core :as core]
    [cu.web :as web]
    [cu.git-test-helpers :refer [git]]
    [environ.core :refer [env]]
    [expectations :refer :all]
    [ring.mock.request :refer [request body header]]
    ))

(defn mkdir-p [path] (sh "mkdir" "-p" path))
(defn create-git-repo [path config]
  (sh "rm" "-rf" path)
  (mkdir-p path)
  (spit (str path "/cu.yml") (yaml/generate-string config))
  (doto path
    (git "init" path)
    (git "add" "-A")
    (git "commit" "-m" "first commit")))

(defn encode64 [string]
  (String. (b64/encode (byte-array (map byte string)))))
(defn auth-headers [request & creds]
  (header request "Authorization"
          (str "Basic " (encode64 (join ":" creds)))))
(defn login [request]
  (auth-headers request (env :cu-username) (env :cu-password)))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (web/app (-> (request :post "/push")
                       (auth-headers request "bad" "credentials")))))

; can view output of pipeline through web interface
(expect-let
  [url "/tmp/cu-test-pipe"
   json-payload (json/write-str {:repository {:name "test-project"
                                              :url  url}})]
  #"(?s)cu\.yml.*cu-test-end/workspace" ; ls and pwd respectively
  (do
    (web/app (-> (request :delete "/logs")
                 login))
    (create-git-repo
      url
      {:bucket "cu-test"
       :log-key "logs"
       :pipeline {:cu-test-start  {:repo   url
                                   :script "ls"}
                  :downstream
                  {:cu-test-end   {:repo   url
                                   :script "pwd"}}}})
    (web/app (-> (request :post "/push")
                 login
                 (body {:payload json-payload})))
    (core/-main "parser")
    (core/-main "worker")
    (:body (web/app (-> (request :get "/logs")
                        login)))))

