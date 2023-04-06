(ns xyloobservations.resizingfunctions
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [xyloobservations.config :refer [env]]
            [xyloobservations.db.core :as db]
            [xyloobservations.mimetypes :as mimetypes]))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn get_dimensions
  "returns a map of width and height"
  [imgpath]
  (let [{:keys [exit out err]} (sh "identify" "-ping" "-format" "%w\n%h\n" imgpath)]
    (if (= exit 0)
      (let [wh (->> out
                    (str/split-lines)
                    (map #(Integer/parseInt %)))]
        {:width (first wh) :height (second wh)})
      (throw (ex-info err
                      {:type :shell-exception, :cause :imagemagic})))))

(defn big_dimension
  "given a map of width and height, returns the biggest dimension"
  [{:keys [width height]}]
  (-> [width height]
      sort
      last))

(defn compresslike
  "resize to resolution"
  [origpath newpath resolution cropsettings]
  (let [cropstring (str (cropsettings :hpercent) "%x"
                        (cropsettings :vpercent) "%+"
                        (cropsettings :hoffset) "+"
                        (cropsettings :voffset))
        resizestring (str resolution "x" resolution ">")
        {:keys [exit out err]} (sh "convert" origpath "-auto-orient"
                                   "-gravity" "Center" "-crop" cropstring
                                   "-quality" "60" "-resize" resizestring
                                   newpath)]
    (when (or
           (not= exit 0)
           (str/includes? err "geometry does not contain image"))
      (throw (ex-info err
                      {:type :shell-exception, :cause :imagemagic})))))

(defn make_image_version
  "Given a path, a max-size and a resolution, makes the image or copy it.
   Return a map of filepath, width, mimetype and identifier.
   n.b. size is the filesize in bytes"
  [{:keys [origpath origdimensions size origmimetype]}
   {:keys [newpath maxsize resolution identifier]}
   cropsettings]
  (if (> size maxsize)
    (let [bigdim (big_dimension origdimensions)]
        ;; don't bother resizing unless the original resolution is substantially
        ;; bigger than the target resolution
      (if (> bigdim (* resolution 1.2))
        (do (compresslike origpath newpath resolution cropsettings)
            (def newdimensions (get_dimensions newpath)))
        (do (compresslike origpath newpath (origdimensions :width) cropsettings)
            (def newdimensions origdimensions)))
      (def newmimetype (str "image/" (env :img-format))))
    (do
      (copy-file origpath newpath)
      (def newdimensions origdimensions)
      (def newmimetype origmimetype)))
  (conj {:filepath newpath
         :mimetype newmimetype
         :extension (mimetypes/type-to-extension newmimetype)
         :identifier identifier} newdimensions))

(defn get_crop_settings
  "given the image resolution & image id, returns the crop settings"
  [{:keys [width height]}
   image_id]
  (let [{croppy :crop_data} (db/get-crop-settings {:image_id image_id})]
    {:hpercent (croppy :hpercent)
     :vpercent (croppy :vpercent)
     :hoffset  (.intValue (* width  (/ (croppy :hoffset) 100)))
     :voffset  (.intValue (* height (/ (croppy :voffset) 100)))}))

(defn resize
  "generates the compressed versions of the uploaded image"
  [size imagebytes image_id mimetype]
  (let [tempdir "/tmp/imageresizing/"
        origpath   (str tempdir image_id "_orig." (mimetypes/type-to-extension mimetype))
        mediumpath (str tempdir image_id "_medium." (env :img-format))
        smallpath  (str tempdir image_id "_small." (env :img-format))
        tinypath   (str tempdir image_id "_tiny." (env :img-format))]

    ;; make the temp directory
    (io/make-parents origpath)

    ;; save original image
    (with-open [w (io/output-stream origpath)]
      (.write w imagebytes))

    (let [origdimensions (get_dimensions origpath)
          cropsettings (get_crop_settings origdimensions image_id)
          make_image_closure #(make_image_version {:origpath origpath
                                                   :origdimensions origdimensions
                                                   :size size
                                                   :origmimetype mimetype} % cropsettings)]
      ;; the output of this function is a map of filepath, mimetype, identifier, width and height
      (conj (map make_image_closure
                 [{:newpath mediumpath :maxsize 1000000 :resolution 2560 :identifier "medium"}
                  {:newpath smallpath  :maxsize 500000  :resolution 1920 :identifier "small"}
                  {:newpath tinypath   :maxsize 250000  :resolution 1280 :identifier "tiny"}])
            ;; we add the details for the original image too
            (conj {:filepath origpath
                   :mimetype mimetype
                   :extension (mimetypes/type-to-extension mimetype)
                   :identifier "original"} origdimensions)))))
