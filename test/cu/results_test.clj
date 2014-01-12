(ns cu.results-test
  (:require
    [expectations :refer :all]
    [cu.results :refer :all]))

(expect "deadbeef/myjob-exit-0"
        (log-key "deadbeef" "myjob" 0))

(expect #{"mypassedjob" "myfailedjob" "slashy/job/name"}
        (job-names [(log-key "deadbeef" "mypassedjob" 0)
                    (log-key "deadbeef" "myfailedjob" 1)
                    (log-key "deadbeef" "slashy/job/name" 0)]))

(expect #{"mypassedjob" "slashy/job/name"}
        (passed-job-names [(log-key "deadbeef" "mypassedjob" 0)
                           (log-key "deadbeef" "myfailedjob" 1)
                           (log-key "deadbeef" "slashy/job/name" 0)
                           (log-key "deadbeef" "an-exit-0-job-name" 1)]))

