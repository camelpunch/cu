(defproject com.camelpunch/cu "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.stuartsierra/component "0.2.1"]
                 [com.taoensso/carmine "2.6.0"]
                 [environ "0.4.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :plugins [[com.jakemccrary/lein-test-refresh "0.4.1"]]
                   :source-paths ["dev"]}})
