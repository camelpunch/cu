(ns cu.web-test
  (:import java.util.UUID)
  (:require
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

; gives 202 response
(expect {:status 202}
        (in
          (do
            (create-git-repo
              "/tmp/cu-test-202"
              {:bucket "cu-test"
               :log-key "logs"
               :workspaces-path "tmp/cu-workspaces"
               :pipeline {:cu-web-test-202 {:repo "/tmp/cu-test-202"
                                 :script "true"}}})
            (let [payload (json/write-str {:repository {:name "foo"
                                                        :url "/tmp/cu-test-202"}})
                  response (web/app (-> (request :post "/push")
                                        login
                                        (body {:payload payload})))]
              ; consume queued item to avoid pollution
              (core/-main "parser")
              (core/-main "worker")
              response))))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (web/app (-> (request :post "/push")
                       (auth-headers request "bad" "credentials")))))

; can view output of pipeline through web interface
(expect-let
  [uuid (str (UUID/randomUUID))
   url "/tmp/cu-test-pipe"
   json-payload (json/write-str {:repository {:name "test-project"
                                              :url  url}})]

  (join "\n"
        ["first command"
         uuid])
  (do
    (create-git-repo
      url
      {:bucket "cu-test"
       :log-key "logs"
       :pipeline {:cu-test-start  {:repo   url
                                   :script "echo 'first command'"}
                  :then
                  {:cu-test-end   {:repo   url
                                   :script (str "echo " uuid)}}}})
    (web/app (-> (request :post "/push")
                 login
                 (body {:payload json-payload})))
    (core/-main "parser")
    (core/-main "worker")
    (:body (web/app (-> (request :get "/logs")
                        login)))))

