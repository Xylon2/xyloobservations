(ns xyloobservations.imagestorefuncs
  (:use
   [amazonica.aws.s3])
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [xyloobservations.config :refer [env]]
   [xyloobservations.queuefunctions :as queue]
   [cheshire.core :refer :all]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn store-image
  "adds an image's basic info to the database and puts it in the queue"
  [extension mimetype tempfile t-conn caption size]
  (let [object_ref (str (.toString (java.util.UUID/randomUUID)))
        image_id (:image_id (db/reference-image! t-conn
                                                 (map-of object_ref caption)))]
    (queue/add tempfile object_ref image_id mimetype size)
    image_id))

(defn resolve_images
  "We output a sequence of maps, each containing an id, caption, a urlprefix and a map of sizes.
   The images argument gives us image_id, object_ref, caption and imagemeta."
  [images]
  (let [url-prefix (env :url-prefix)]
    (for [x images]
      {:image_id (x :image_id)
       :caption (x :caption)
       :urlprefix (str url-prefix (x :object_ref))
       :sizes (x :imagemeta)})))
