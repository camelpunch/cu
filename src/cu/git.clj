(ns cu.git
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [split]]
    [clj-yaml.core :as yaml]
    ))

(defn fresh-clone
  [repo dest]
  (println "CLONING" repo "TO" dest)
  (sh "rm" "-r" dest)
  (println (sh "git" "clone" repo dest))

  (let [config-path (str dest "/cu.yml")]
    (println "GOT CONFIG PATH" config-path)
    {:name    (last (split dest #"/"))
     :config  (-> config-path slurp yaml/parse-string)}))

