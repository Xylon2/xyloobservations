(ns xyloobservations.imagestorefuncs
  (:use
   [amazonica.aws.s3])
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [xyloobservations.config :refer [env]]
   [xyloobservations.queuefunctions :as queue]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn store-image
  "stores an image using whichever backend is appropriate"
  [extension mimetype tempfile t-conn caption]
  (case (env :image-store)
    "s3"
    (let [object_ref (str (.toString (java.util.UUID/randomUUID)) "." extension)
          image_id (:image_id (db/reference-image! t-conn
                                                   (map-of object_ref mimetype caption)))]
      (queue/add tempfile object_ref image_id mimetype)
      image_id)
    "postgres"
    (comment (let [imagedata (slurp-bytes tempfile)]
               (:image_id (db/upload-image! t-conn
                                            (map-of imagedata mimetype caption)))))))

(defn resolve_images
  "we output a sequence of maps, each containing a URL and a caption"
  [images]
  (case (env :image-store)
    "s3"
    (let [url-prefix (env :url-prefix)]
      (for [x images]
        {:image_id (x :image_id)
         :caption (x :caption)
         :url (str url-prefix (x :object_ref))}))
    "postgres"
    (for [x images]
      {:image_id (x :image_id)
       :caption (x :caption)
       :url (str "/image?id=" (x :image_id))})))

(defn resolve_image
  "we output the url an image can be accessed at"
  [image_id object_ref]
  (case (env :image-store)
    "s3"
    (str (env :url-prefix) object_ref)
    "postgres"
    (str "/image?id=" image_id)))
