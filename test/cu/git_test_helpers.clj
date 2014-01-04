(ns cu.git-test-helpers
  (:require
    [clojure.java.shell :refer [sh]]))

(defn git [dir & cmds] (:out (apply sh (concat ["git"
                                                 (str "--git-dir=" (str dir "/.git"))
                                                 (str "--work-tree=" dir)]
                                                cmds))))
