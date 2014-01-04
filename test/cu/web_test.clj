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
    [environ.core :refer [env]]
    [expectations :refer :all]
    [ring.mock.request :refer [request body header]]
    ))

(def git-repo-path "/tmp/cu-web-test")

(defn chmod+x [file] (sh "chmod" "+x" file))
(defn mkdir-p [path] (sh "mkdir" "-p" path))

(defn git-dir [dir] (str dir "/.git"))
(defn option [k v] (join "=" [(str "--" k) v]))
(defn git [dir & cmds] (:out (apply sh (concat ["git"
                                                (->> dir git-dir (option "git-dir"))
                                                (->> dir (option "work-tree"))]
                                               cmds))))
(defn git-init [dir] (git dir "init" dir))
(defn write-executable [base-path filename contents]
  (let [full-path (str base-path "/cu/" filename)]
    (spit full-path contents)
    (chmod+x full-path)))
(defn rm-git-dir [parent-dir]
  (sh "rm" "-r" (git-dir parent-dir)))
(defn prepare-script-dir [parent-dir]
  (doto (str parent-dir "/cu")
    #(sh "rm" "-rf" %)
    mkdir-p))

(defn create-git-repo
  [path script-filename script]
  (doto path
    rm-git-dir
    prepare-script-dir
    (write-executable script-filename script)
    git-init
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
          (let [payload (json/write-str
                          {:repository {:name "foo"
                                        :url (create-git-repo git-repo-path
                                                              "00-test1"
                                                              "foo")}})
                response (web/app (-> (request :post "/push")
                                      login
                                      (body {:payload payload})))]
            ; consume queued item to avoid pollution
            (core/-main)
            response)))

; 401s with incorrect auth
(expect {:status 401}
        (in
          (web/app (-> (request :post "/push")
                       (auth-headers request "bad" "credentials")))))

; can view output of command through web interface
(expect-let [script-filename "00-run-pipeline"
             evidence-that-command-ran (str (UUID/randomUUID))
             script (str "echo " evidence-that-command-ran)
             json-payload (json/write-str
                            {:repository {:name "test-project"
                                          :url git-repo-path}})]

            (re-pattern evidence-that-command-ran)
            (do
              (create-git-repo git-repo-path script-filename script)
              (web/app (-> (request :post "/push")
                           (login)
                           (body {:payload json-payload})))
              (core/-main)
              (:body (web/app (-> (request :get "/logs")
                                  (login))))))

