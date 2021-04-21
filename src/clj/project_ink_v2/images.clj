(ns project-ink-v2.images
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [mikera.image.core :as image]
            [clojure.pprint :refer [pprint]]
            [mount.core :refer [defstate]]))

(def c (a/chan))

(defn take-image-n-resize
  "
  What is needed here?

  The file, filename, path to store?

  todo: spec
  "
  [file]
  (try
    (when-let [img-file (:tempfile file)]
      (-> (image/load-image img-file)
          (image/resize 150)
          (image/write (str "uploads/" (:filename file) "-small.jpg") "png")))
    (catch Throwable t
      (println (.getMessage t)))))

(def worker
  (future
    (a/go-loop []
      (let [file (a/<! c)]
        (println "got a value in this loop: " file)
        (take-image-n-resize file))
      (recur))))

(a/put! c 1)

(defn >img-to-resize!
  "
  Todo: spec the function
  "
  [img]
  (println "resize image: " img)
  (a/put! c img))

(comment
  (identity worker)
  (future-done? worker)
  (future-cancelled? worker)
  (future-cancel worker)
  (deref worker))

(comment

  ;; with an image file
  (def image-file (io/resource "img/test-img.jpg"))

  (>img-to-resize! {:tempfile image-file})
  ;; resize the image
  (image/resize (image/load-image image-file) 200)

  ;; resize the image and write to a file
  (image/write
   (image/resize (image/load-image image-file) 200)
   "resized.jpg"
   "png")

  (def w 40)
  (-> (io/resource "img/test-img.jpg") ;; get file
      image/load-image                ;; load file as image
      (image/resize w)                 ;; resize image to width
      (image/write "resized.jpg" "png"));; write image to a file
  )
