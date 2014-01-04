(ns cu.git
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.java.io :refer [file]]
    ))

(defn- ls-scripts [basedir]
  (let [script-dir (str basedir "/cu/")]
    (map #(.getCanonicalPath %) (-> script-dir file .listFiles))))

(defn fresh-clone
  [repo dest]
  (sh "rm" "-r" dest)
  (sh "git" "clone" repo dest)

  {:scripts (ls-scripts dest)})

