(ns xyloobservations.homefunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]))

(defn images-with-tags []
  "any images that have tags attached.
   output is image_id, object_ref, caption and imagemeta"
  (distinct (db/images-with-tags)))

(defn matching-images
   "get a list of images that have all of a list of tags.
    output is image_id, object_ref, caption and imagemeta"
  [tags]
  (db/images-multi-tags {:tags (vec tags)}))

(defn always-vector
  "this function takes something which may or may not be a vector and makes it always a vector"
  [item]
  (cond
    (= (type item) clojure.lang.PersistentVector) item
    (nil? item) []
    :else [item]))

(defn default-number
  "if it's null, returns string 10"
  [item]
  (cond
    (= (type item) java.lang.String) item
    (nil? item) "10"))
