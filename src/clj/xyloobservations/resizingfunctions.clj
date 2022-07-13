(ns xyloobservations.resizingfunctions
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn get_dimensions
  "returns a map of width and height"
  [imgpath]
  (let [wh (->> (sh "identify" "-ping" "-format" "%w\n%h\n" imgpath)
                :out
                (str/split-lines)
                (map #(Integer/parseInt %)))]
    {:width (first wh) :height (second wh)}))

(defn big_dimension
  "given a map of width and height, returns the biggest dimension"
  [{:keys [width height]}]
  (-> [width height]
      sort
      last))

(defn compresslike
  "resize to resolution"
  [origpath newpath resolution]
  (sh "convert" origpath "-quality" "60" "-resize" (str resolution "x" resolution ">") "-define" "webp:method=6" newpath))

(defn make_image_version
  "Given a path, a max-size and a resolution, makes the image or copy it.
   Return a map of filepath, width, mimetype and identifier."
  [{:keys [origpath origdimensions size origmimetype newpath maxsize resolution identifier]}]
  (if (> size maxsize)
    (let [bigdim (big_dimension origdimensions)]
        ;; don't bother resizing unless the original resolution is substantially
        ;; bigger than the target resolution
      (if (> bigdim (* resolution 1.2))
        (do (compresslike origpath newpath resolution)
            (def newdimensions (get_dimensions newpath)))
        (do (compresslike origpath newpath (origdimensions :width))
            (def newdimensions origdimensions)))
      (def newmimetype "image/webp"))
    (do
      (copy-file origpath newpath)
      (def newdimensions origdimensions)
      (def newmimetype origmimetype)))
  (conj {:filepath newpath :mimetype newmimetype :identifier identifier} newdimensions))

(defn resize
  "generates the compressed versions of the uploaded image"
  [size imagebytes image_id mimetype]
  (let [tempdir "/tmp/imageresizing/"
        origpath   (str tempdir image_id "_orig")  ;; no extension as IM auto-detects type
        mediumpath (str tempdir image_id "_medium.webp")
        smallpath  (str tempdir image_id "_small.webp")
        tinypath   (str tempdir image_id "_tiny.webp")]

    ;; make the temp directory
    (io/make-parents origpath)

    ;; save original image
    (with-open [w (io/output-stream origpath)]
      (.write w imagebytes))

    (let [origdimensions (get_dimensions origpath)]
      ;; the output of this function is a map of filepath, mimetype, identifier, width and height
      (conj (map #(make_image_version (conj {:origpath origpath
                                             :origdimensions origdimensions
                                             :size size
                                             :origmimetype mimetype} %))
                 [{:newpath mediumpath :maxsize 1000000 :resolution 2560 :identifier "medium"}
                  {:newpath smallpath  :maxsize 500000  :resolution 1920 :identifier "small"}
                  {:newpath tinypath   :maxsize 250000  :resolution 1280 :identifier "tiny"}])
            (conj {:filepath origpath
                   :mimetype mimetype
                   :identifier "original"} origdimensions)))))