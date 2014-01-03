(ns cu.payload)

(defn clone-target-url [payload]
  (if payload (get-in (read-string payload) ["repository" "url"])))

