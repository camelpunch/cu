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

(def git-repo-path "/tmp/cu-web-test")

(defn chmod+x [file] (sh "chmod" "+x" file))
(defn mkdir-p [path] (sh "mkdir" "-p" path))

(defn write-script [base-path filename contents]
  (let [full-path (str base-path "/cu/" filename)]
    (spit full-path contents)
    (chmod+x full-path)))

(defn write-scripts [basepath script-filenames]
  (doseq [[filename script] script-filenames]
    (write-script basepath filename script)))

(defn create-git-repo [path & script-filenames]
  (sh "rm" "-rf" path)
  (mkdir-p (str path "/cu"))
  (doto path
    (write-scripts (partition-all 2 script-filenames))
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
            (create-git-repo git-repo-path
                             "00-test1" "foo")
            (let [payload (json/write-str {:repository {:name "foo"
                                                        :url git-repo-path}})
                  response (web/app (-> (request :post "/push")
                                        login
                                        (body {:payload payload})))]
              ; consume queued item to avoid pollution
              (core/-main)
              response))))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (web/app (-> (request :post "/push")
                       (auth-headers request "bad" "credentials")))))

; can view output of pipeline through web interface
(expect-let [evidence-that-command-ran (str (UUID/randomUUID))
             json-payload (json/write-str
                            {:repository {:name "test-project"
                                          :url git-repo-path}})]

            (str "running first command"    "\n"
                 evidence-that-command-ran  "\n")
            (do
              (create-git-repo
                git-repo-path
                "00-start"  "echo 'running first command'; exit 0"
                "01-finish" (str "echo " evidence-that-command-ran))
              (web/app (-> (request :post "/push")
                           (login)
                           (body {:payload json-payload})))
              (core/-main)
              (:body (web/app (-> (request :get "/logs")
                                  (login))))))

