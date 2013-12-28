(ns cu.web-test
  (:import java.util.UUID)
  (:require [expectations :refer :all]
            [ring.mock.request :refer [request body]]
            [environ.core :refer [env]]
            [cu.web :refer [app]]
            [clojure.string :refer [join trim-newline]]
            [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]))

(def git-repo-path "/tmp/cu-web-test")

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

; gives 201 response
(expect {:status 201}
        (in
          (app (body (request :post "/push")
                     {:payload
                      (json/write-str
                        {:repository
                         {:name "foo"
                          :url (create-git-repo git-repo-path "run-pipeline" "foo")}})}))))

; writes output of requested command to a log file
(expect-let [evidence-that-command-ran (str (UUID/randomUUID))

             script (str "echo " evidence-that-command-ran)

             script-filename "run-pipeline"

             json-payload (json/write-str
                            {:repository {:name "test-project"
                                          :url git-repo-path}})

             log-path (str (env :workspaces-path) "/test-project/log")]

            evidence-that-command-ran
            (do
              (create-git-repo git-repo-path script-filename script)
              (sh "rm" log-path)
              (app (body (request :post "/push") {:payload json-payload}))
              (-> log-path slurp trim-newline)))

