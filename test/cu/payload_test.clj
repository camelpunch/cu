(ns cu.payload-test
  (:require
    [cu.payload :refer :all]
    [expectations :refer :all]
    ))

;; Clone target URL parsing

; can grab URL from a push payload
(expect "http://foo.bar"
        (clone-target-url (str {"repository" {"url" "http://foo.bar"}})))

; ignores empty queue payloads when getting clone target URLs
(expect nil (clone-target-url "{}"))


;; Message status - whether to:
;;  move to immediate and delete (run)
;;  delete only (delete)
;;  leave in queue (wait)
(def message-for-job-with-no-upstream-jobs
  {:body (pr-str {:name     "test-web-app-units"
                  :script   "true"
                  :repo     "https://some.repo"
                  :upstream []})})

(def message-for-job-with-upstream-jobs
  {:body (pr-str {:name     "test-mobile-against-web"
                  :script   "true"
                  :repo     "https://some.repo"
                  :upstream ["deploy-web" "test-mobile-units"]})})

; should run job with no upstream jobs
(expect :run
        (next-job-action message-for-job-with-no-upstream-jobs
                         (fn [_] (assert false "Should never be called"))))

; should run job with upstream jobs that all passed
(expect :run
        (next-job-action message-for-job-with-upstream-jobs
                         (fn [job] (job :upstream))))

; should delete job with some failed upstream jobs
(expect :delete
        (next-job-action message-for-job-with-upstream-jobs
                         (fn [job] [(first (job :upstream))])))

; should wait for job with no run jobs
(expect :wait
        (next-job-action message-for-job-with-upstream-jobs
                         (fn [_] nil)))


;; Non-payload stuff that should be moved to 'config' namespace

; pull immediate jobs from a pipeline config
(expect
  [{:name     "test-web-app-units"
    :script   "true"
    :repo     "https://some.repo"
    :upstream []}
   {:name     "test-web-app-integrations"
    :script   "exit 0"
    :repo     "https://some.repo"
    :upstream []}]
  (immediate-jobs
    {:test-web-app-units        {:script  "true"
                                 :repo    "https://some.repo"}
     :test-web-app-integrations {:script  "exit 0"
                                 :repo    "https://some.repo"}
     :downstream {:something-else {}}}))

; pull waiting jobs from a pipeline config
(expect-focused
  [{:name     "deploy-website-staging"
    :script   "true"
    :repo     "https://web.repo"
    :upstream ["some-initial-job" "another-initial-job"]}
   {:name     "deploy-website-qa"
    :script   "true"
    :repo     "https://web.repo"
    :upstream ["some-initial-job" "another-initial-job"]}
   {:name     "test-mobile-app-against-staging"
    :script   "rake"
    :repo    "https://some.mobile.repo"
    :upstream ["deploy-website-staging" "deploy-website-qa"]}]
  (waiting-jobs
    {:some-initial-job         {}
     :another-initial-job      {}
     :downstream {:deploy-website-staging  {:script  "true"
                                            :repo    "https://web.repo"}
                  :deploy-website-qa       {:script  "true"
                                            :repo    "https://web.repo"}
                  :downstream {:test-mobile-app-against-staging {:script "rake"
                                                                 :repo   "https://some.mobile.repo"}}}}))

; empty seqs of jobs for nil args
(expect [] (immediate-jobs nil))
(expect [] (waiting-jobs nil))
