(ns project-ink-v2.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[project-ink-v2 started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[project-ink-v2 has shut down successfully]=-"))
   :middleware identity})
