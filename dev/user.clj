(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.camelpunch.cu :as app]
   [com.camelpunch.cu.queue-redis :as q]
   [com.stuartsierra.component :as component]
   [environ.core :refer [env]]
   [taoensso.carmine :as car :refer [wcar]]
   [taoensso.carmine.message-queue :refer [enqueue worker queue-status]]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system
                  (constantly (app/build-system))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn t
  []
  (refresh)
  (clojure.test/run-tests 'com.camelpunch.queue-redis-test
                          'com.camelpunch.web-test))

(def redisconn {:pool {}
                :spec {:host "127.0.0.1"
                       :port 6379}})

(defn build-queue
  []
  (-> system
      :build-queue))
