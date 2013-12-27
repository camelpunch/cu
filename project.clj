(defproject cu "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [environ "0.4.0"]
                 [org.clojure/data.json "0.2.3"]
                 [ring/ring-jetty-adapter "1.1.0"]]
  :plugins [[lein-ring "0.8.8"]
            [lein-environ "0.4.0"]
            [lein-expectations "0.0.7"]
            [lein-autoexpect "1.2.1"]]
  :ring {:handler cu.web/app}
  :profiles {:dev
             {:dependencies [[expectations "1.4.52"]
                                  [ring-mock "0.1.5"]]
             :env {:workspaces-path "/tmp/cu-workspaces"}}})
