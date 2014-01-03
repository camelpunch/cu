(ns cu.payload-test
  (:require
    [cu.payload :as payload]
    [expectations :refer :all]
    ))

; grabs URL from payloads
(expect "http://foo.bar"
        (payload/clone-target-url (str {"repository" {"url" "http://foo.bar"}})))
;
; ignores empty payloads
(expect nil (payload/clone-target-url "{}"))

