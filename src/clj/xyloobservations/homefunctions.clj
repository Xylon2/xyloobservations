(ns xyloobservations.homefunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]))

(defn get-orphan-images []
  (db/orphan-images))

(defn add-images [tag]
  "given a map of values for the tag, add a extra key containing a vector of maps of imageids and captions"
  (let [tag_id (tag :tag_id)]
    (assoc tag :images (db/images-by-tag {:tag_ref tag_id}))))

(defn tags-with-images []
  ":tag_id :tag_name and :description"
  (map add-images (set (db/tags-with-images)))
  )

(defn fetch-image [image_id]
  "just run the fetch-image query and convert the imagedata into an io/input-stream"
  (let [{:keys [imagedata mimetype]} (db/fetch-image {:image_id image_id})]
    {:mimetype mimetype :imagedata (io/input-stream imagedata)}))

(defn images-with-tags []
  (distinct (db/images-with-tags)))

(defn filter-images [[firsttag & tags] images]
  "repeatedly filter down till we run out of tags"
  (let [image_ids (map :image_id images)]
    (if (= firsttag nil)
      images
      (filter-images tags (db/filter-images {:tag firsttag :images image_ids})))))

(defn matching-images [[firsttag & tags]]
  "get a list of images that have the first tag"
  (let [images (db/images-by-tag {:tag_ref firsttag})]
    (filter-images tags images)))

(defn always-vector [item]
  "this function takes something which may or may not be a vector and makes it always a vector"
  (if (= (type item) clojure.lang.PersistentVector)
    item
    (if (= (type item) nil)
      []
      [item])))
