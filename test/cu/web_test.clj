(ns cu.web-test
  (:import java.util.UUID)
  (:require
    [cemerick.bandalore :as sqs]
    [clj-yaml.core :as yaml]
    [clojure.data.codec.base64 :as b64]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [join split trim-newline]]
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

(defn commit-file-to-repo [path filename]
  (spit (str path "/" filename) "")
  (doto path
    (git "add" "-A")
    (git "commit" "-m" "second commit")))

(defn encode64 [string]
  (String. (b64/encode (byte-array (map byte string)))))

(defn auth-headers [request & creds]
  (header request "Authorization"
          (str "Basic " (encode64 (join ":" creds)))))

(defn authenticator [config]
  (fn [request]
    (auth-headers request (config :cu-username) (config :cu-password))))

(defn shell-output [command]
  (trim-newline (:out (sh command))))

(def test-config
  (let [uuid (UUID/randomUUID)]
    {:aws-credentials {:access-key (env :aws-access-key)
                       :secret-key (env :aws-secret-key)}
     :cu-username "test-username"
     :cu-password "test-password"
     :queue-period 1
     :queue-max-wait 1
     :bucket "cu-test"
     :log-key "logs"
     :push-queue (str "cu-pushes-test-" uuid)
     :build-queue (str "cu-builds-test-" uuid)
     ; TODO: separate web config from parser / worker config
     :workspaces-path "tmp/cu-workspaces"}))

(def web-app (web/app test-config))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (web-app (-> (request :post "/push")
                       (auth-headers "bad" "credentials")))))

(defn slow-pipeline-run []
  (let [log-output (let [repo-url  "/tmp/cu-test-pipe"
                         json-payload  (json/write-str {:repository {:name "test-project"
                                                                     :url  repo-url}})
                         logged-in     (authenticator test-config)]
                     (web-app (-> (request :delete "/logs") logged-in))
                     (create-git-repo repo-url
                                      {:pipeline
                                       {:ls  {:repo   repo-url
                                              :script "ls"}
                                        :downstream   {:whoami     {:repo    repo-url
                                                                    :script  "whoami"}
                                                       :hostname   {:repo    repo-url
                                                                    :script  "hostname"}}}})
                     ; enqueues stuff to pushes queue
                     (web-app (-> (request :post "/push") logged-in
                                  (body {:payload json-payload})))

                     ; enqueues stuff to builds queue
                     (core/parser test-config)

                     (commit-file-to-repo repo-url "already-parsed-so-should-not-appear")

                     ; processes builds queue
                     (core/worker test-config)

                     (println "Sleeping for visibility timeout")
                     (Thread/sleep 1000)

                     ; try to process the rest (sometimes unnecessary)
                     (core/worker test-config)

                     (web-app (-> (request :delete "/queues") logged-in))
                     (-> (web-app (-> (request :get "/logs") logged-in))
                         :body
                         (split #"\n")))]
    {:number-of-log-lines (count log-output)
     :first-line          (first log-output)
     :rest-of-lines       (set (rest log-output))}))

(expect
  {:number-of-log-lines 3
   :first-line          "cu.yml"
   :rest-of-lines       #{(shell-output "whoami") (shell-output "hostname")}}
  (slow-pipeline-run))

