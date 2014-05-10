(defproject com.camelpunch/cu "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [com.stuartsierra/component "0.2.1"]
                 [compojure "1.1.6"]
                 [com.taoensso/carmine "2.6.0"]
                 [environ "0.4.0"]
                 [ring/ring-jetty-adapter "1.1.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [ring-mock "0.1.5"]]
                   :plugins [[com.jakemccrary/lein-test-refresh "0.4.1"]]
                   :source-paths ["dev"]}})
