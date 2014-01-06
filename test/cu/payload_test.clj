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
(expect '({:name    "test-web-app-units"
           :script  "true"
           :repo    "https://some.repo"}
          {:name    "test-web-app-integrations"
           :script  "exit 0"
           :repo    "https://some.repo"})
        (payload/immediate-jobs
          {:pipeline {:test-web-app-units         {:script  "true"
                                                   :repo    "https://some.repo"}
                      :test-web-app-integrations  {:script  "exit 0"
                                                   :repo    "https://some.repo"}
                      :then {:something-else {}}}}))

