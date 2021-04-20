(ns project-ink-v2.images
  (:require [clojure.core.async :as a]
            [mount.core :refer [defstate]]))

(def c (a/chan))

(def worker
  (future
    (a/go-loop []
      (let [x (a/<! c)]
        (println "got a value in this loop: " x))
      (recur))))

(a/put! c 1)

(defn >img-to-resize! [img]
  (a/put! c img))

(comment
  (identity worker)
  (future-done? worker)
  (future-cancelled? worker)
  (future-cancel worker)
  (deref worker))
