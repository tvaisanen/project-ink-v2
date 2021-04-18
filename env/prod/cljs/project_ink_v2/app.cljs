(ns project-ink-v2.app
  (:require [project-ink-v2.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
