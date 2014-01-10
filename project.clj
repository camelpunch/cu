(defproject cu "0.1.0-SNAPSHOT"
  :aot [cu.core]
  :main cu.core

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [clj-aws-s3 "0.3.7"]
                 [clj-time "0.6.0"]
                 [clj-yaml "0.4.0"]
                 [com.cemerick/bandalore "0.0.5"]
                 [compojure "1.1.6"]
                 [environ "0.4.0"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.3"]
                 [ring-basic-authentication "1.0.2"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 ]
  :plugins [
            [lein-autoexpect "1.2.1"]
            [lein-environ "0.4.0"]
            [lein-expectations "0.0.7"]
            [lein-ring "0.8.8"]
            ]
  :ring {:handler cu.web/app}
  :profiles {:dev
             {:dependencies [
                             [expectations "1.4.52"]
                             [ring-mock "0.1.5"]
                             ]
              :env {; SQS settings tweaked for automated tests - override in
                    ; ~/.lein/profiles.clj
                    :cu-period "1"
                    :cu-max-wait "1"}}})
