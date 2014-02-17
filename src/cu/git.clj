(ns cu.git
  (:require
    [clojure.java.shell :refer [sh with-sh-dir]]
    [clojure.string :refer [split trim-newline]]
    [clj-yaml.core :as yaml]
    ))

(defn fresh-clone
  [repo dest git-ref]
  (println "CLONING" repo "TO" dest)
  (sh "rm" "-r" dest)
  (println (sh "git" "clone" repo dest))
  (with-sh-dir dest
    (println (sh "git" "checkout" git-ref))

    (let [config-path (str dest "/cu.yml")]
      (println "GOT CONFIG PATH" config-path)
      {:name    (last (split dest #"/"))
       :ref     (trim-newline (:out (sh "git" "show-ref" "-s"
                                        (str "heads/" git-ref))))
       :config  (-> config-path slurp yaml/parse-string)})))

