(ns xyloobservations.homefunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]))

(defn get-orphan-images []
  (db/orphan-images))

(defn add-images [tag]
  "given a map of values for the tag, add a extra key containing a vector of imageids"
  (let [tag_id (tag :tag_id)]
    (assoc tag :images (map :image_ref (db/images-by-tag {:tag_ref tag_id})))))

(defn tags-with-images []
  ":tag_id :tag_name and :description"
  (map add-images (set (db/tags-with-images)))
  )

(defn fetch-image [image_id]
  (-> {:image_id image_id}
      db/fetch-image
      :imagedata
      io/input-stream))
