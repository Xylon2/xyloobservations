(ns xyloobservations.imagestorefuncs
  (:use
   [amazonica.aws.s3])
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [xyloobservations.config :refer [env]]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn awscreds
  "makes a map of aws creds in the format put-object likes"
  []
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)
   :endpoint (env :aws-region)})

(defn store-image
  "stores an image using whichever backend is appropriate"
  [extension mimetype tempfile t-conn caption]
  (spit "/home/joseph/cljdebug.txt" awscreds)
  (case (env :image-store)
    "s3"
    (let [object_ref (str (.toString (java.util.UUID/randomUUID)) "." extension)]
      (put-object awscreds
                  :bucket-name (env :bucket-name)
                  :key object_ref
                  :metadata {:content-type mimetype
                             :cache-control "public, max-age=31536000, immutable"}
                  :file tempfile)
      (:image_id (db/reference-image! t-conn
                                      (map-of object_ref mimetype caption))))
    "postgres"
    (let [imagedata (slurp-bytes tempfile)]
      (:image_id (db/upload-image! t-conn
                                   (map-of imagedata mimetype caption))))))

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
