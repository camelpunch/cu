(ns cu.git
  (:require
    [clojure.java.shell :refer [sh]]
    ))

(defn fresh-clone
  [repo dest]
  (sh "rm" "-r" dest)
  (sh "git" "clone" repo dest))

