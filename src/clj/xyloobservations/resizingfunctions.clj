(ns xyloobservations.resizingfunctions
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn big_dimension
  "returns the biggest dimension"
  [imgpath]
  (->> (sh "identify" "-ping" "-format" "%w\n%h\n" imgpath)
       :out
       (str/split-lines)
       (map #(Integer/parseInt %))
       sort
       last))

(defn compresslike
  "resize to resolution"
  [origpath newpath resolution]
  (sh "convert" origpath "-quality" "60" "-resize" (str resolution "x" resolution ">") newpath))

(defn compress_images
  [origpath newpath resolution]
  (let [bigdim (big_dimension origpath)]
    (if (> bigdim (* resolution 1.2))
      (compresslike origpath newpath resolution)
      (compresslike origpath newpath bigdim))))

(defn resize
  "generates the compressed versions of the uploaded image"
  [size imagebytes image_id mimetype]
  (let [origpath (str "/tmp/imageresizing/" image_id "_orig")  ;; no extension as IM auto-detects type
        mediumpath (str "/tmp/imageresizing/" image_id "_medium.avif")
        smallpath (str "/tmp/imageresizing/" image_id "_small.avif")]
    (io/make-parents origpath)

    ;; save original image
    (with-open [w (io/output-stream origpath)]
      (.write w imagebytes))

    ;; make medium image if it's bigger than 1MB
    (if (> size 1000000)
      (compress_images origpath mediumpath 2560)
      (copy-file origpath mediumpath))

    ;; make small image if it's bigger than 300KB
    (if (> size 300000)
      (compress_images origpath smallpath 1920)
      (copy-file origpath smallpath))

    [{:filepath origpath :mimetype mimetype :identifier "original"}
     {:filepath mediumpath :mimetype "image/avif" :identifier "medium"}
     {:filepath smallpath :mimetype "image/avif" :identifier "small"}]))
