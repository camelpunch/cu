(ns cu.web-test
  (:import java.util.UUID)
  (:require
    [aws.sdk.s3 :as s3]
    [clojure.data.codec.base64 :as b64]
    [clojure.data.json :as json]
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [join trim-newline]]
    [cu.web :refer [app]]
    [cu.core :as worker]
    [environ.core :refer [env]]
    [expectations :refer :all]
    [ring.mock.request :refer [request body header]]
    ))

(def git-repo-path "/tmp/cu-web-test")
(def aws-creds {:access-key (env :aws-access-key)
                :secret-key (env :aws-secret-key)})
(def bucket (env :log-bucket))
(def log-key (env :log-key))

(defn chmod+x [file] (sh "chmod" "+x" file))
(defn git-dir [dir] (str dir "/.git"))
(defn rm-git-dir [parent-dir]
  (sh "rm" "-r" (git-dir parent-dir)))
(defn option [k v] (join "=" [(str "--" k) v]))
(defn git [dir & cmds] (:out (apply sh (concat ["git"
                                                (->> dir git-dir (option "git-dir"))
                                                (->> dir (option "work-tree"))]
                                               cmds))))
(defn git-init [dir] (git dir "init" dir))
(defn write-executable [base-path filename contents]
  (let [full-path (str base-path "/" filename)]
    (spit full-path contents)
    (chmod+x full-path)))

(defn mkdir-p [path] (sh "mkdir" "-p" path))

(defn create-git-repo
  [path script-filename script]
  (doto path
    mkdir-p
    rm-git-dir
    (write-executable script-filename script)
    git-init
    (git "add" script-filename)
    (git "commit" "-m" "first commit")))

(defn encode64 [string]
  (String. (b64/encode (byte-array (map byte string)))))

(defn auth-headers [request & creds]
  (header request "Authorization"
          (str "Basic " (encode64 (join ":" creds)))))
(defn correct-auth-headers [request]
  (auth-headers request (env :cu-username) (env :cu-password)))

; gives 202 response
(expect {:status 202}
        (in
          (app (-> (request :post "/push")
                   (correct-auth-headers)
                   (body {:payload
                          (json/write-str
                            {:repository
                             {:name "foo"
                              :url (create-git-repo git-repo-path
                                                    "run-pipeline"
                                                    "foo")}})})))))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (app (-> (request :post "/push")
                   (auth-headers request "bad" "credentials")))))

; can view output of command through web interface
(expect-let [evidence-that-command-ran (str (UUID/randomUUID))

             script (str "echo " evidence-that-command-ran)

             script-filename "run-pipeline"

             json-payload (json/write-str
                            {:repository {:name "test-project"
                                          :url git-repo-path}})]

            (re-pattern evidence-that-command-ran)
            (do
              (create-git-repo git-repo-path script-filename script)
              (app (-> (request :post "/push")
                       (correct-auth-headers)
                       (body {:payload json-payload})))
              (worker/run (env :cu-queue-name))
              (:body (app (-> (request :get "/logs")
                              (correct-auth-headers))))))

