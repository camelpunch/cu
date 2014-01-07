(ns cu.payload-test
  (:require
    [cu.payload :as payload]
    [expectations :refer :all]
    ))

; grabs URL from a queue payload
(expect "http://foo.bar"
        (payload/clone-target-url (str {"repository" {"url" "http://foo.bar"}})))

; ignores empty queue payloads when getting clone target URLs
(expect nil (payload/clone-target-url "{}"))

; pulls immediate jobs from a partial webhook payload
(expect
  [{:name    "test-web-app-units"
    :script  "true"
    :repo    "https://some.repo"}
   {:name    "test-web-app-integrations"
    :script  "exit 0"
    :repo    "https://some.repo"}]
  (payload/immediate-jobs
    {:test-web-app-units        {:script  "true"
                                 :repo    "https://some.repo"}
     :test-web-app-integrations {:script  "exit 0"
                                 :repo    "https://some.repo"}
     :downstream {:something-else {}}}))

; pulls waiting jobs from a partial webhook payload
(expect
  [{:name    "deploy-website-staging"
    :script  "true"
    :repo    "https://web.repo"}
   {:name    "deploy-website-qa"
    :script  "true"
    :repo    "https://web.repo"}
   {:name    "test-mobile-app-against-staging"
    :script  "rake"
    :repo    "https://some.mobile.repo"}]
  (payload/waiting-jobs
    {:some-initial-job         {}
     :another-initial-job      {}
     :downstream {:deploy-website-staging  {:script  "true"
                                            :repo    "https://web.repo"}
                  :deploy-website-qa       {:script  "true"
                                            :repo    "https://web.repo"}
                  :downstream {:test-mobile-app-against-staging {:script "rake"
                                                                 :repo   "https://some.mobile.repo"}}}}))

; empty seqs of jobs for nil args
(expect [] (payload/immediate-jobs nil))
(expect [] (payload/waiting-jobs nil))
