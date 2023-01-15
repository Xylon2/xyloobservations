(ns xyloobservations.adminfunctions
  (:use
   [amazonica.aws.s3])
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.string :as str]
   [cheshire.core :refer [generate-string]]
   [xyloobservations.imagestorefuncs :as imgstore]
   [xyloobservations.mimetypes :as mimetypes]
   [xyloobservations.homefunctions :as homefunc]
   [xyloobservations.queuefunctions :as queuefunc]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn add-tag! [tagname description advanced]
  (when (some empty? [tagname description])
    (throw (AssertionError. "empty values are not allowed")))
  (db/add-tag! (map-of tagname description advanced)))

(defn modify_tag [tag_id, tag_name, description, advanced]
  (when (some empty? [tag_name description])
    (throw (AssertionError. "empty values are not allowed")))
  (db/modify-tag! (map-of tag_id tag_name description advanced)))

(defn upload-image! [{{:keys [tempfile size filename]} "filename"}
                     caption
                     chozen_tags]
  ;; size is the filesize in bytes
  (let [extension (last (str/split filename #"\."))
        mimetype (mimetypes/extension-to-type extension)
        chozen_tags' (homefunc/always-vector chozen_tags)]
    (when (not mimetype)
      (throw (AssertionError. "cannot detect file-type based on extension")))
    (when (> size 20000000)
      (throw (AssertionError. "this picture is too big")))
    (jdbc/with-transaction [t-conn db/*db*]
      (let [image_id (imgstore/store-image extension mimetype tempfile t-conn caption size)]
        (when-not (empty? chozen_tags')
          (db/tag-image! t-conn {:taglist chozen_tags'
                                 :image_id image_id}))
        image_id))))

(defn tag-image! "assign a tag to an image"
  [image_id {{tag_id :tag} :params}]
  (when (-> (db/find-imagetag (map-of tag_id image_id))
            :count
            (= 0)
            not)
    (throw (AssertionError. "this tag is already assigned to this image")))
  (db/tag-image! {:taglist [tag_id] :image_id image_id}))

(defn untag-image! "un-assign a tag from an image"
  [image_id {{tag_id :tag} :params}]
  (db/untag-image! {:tag tag_id :image_id image_id}))

(defn update-caption! "simply update the caption"
  [image_id {{newcaption :caption} :params}]
  (db/update-caption! (map-of newcaption image_id)))

(defn crop-image "update the image crop setting, and trigger the pipeline to re-compress the image"
  [image_id, {{:keys [hpercent vpercent hoffset voffset]} :params}]
  (db/set-crop! {:image_id image_id
                 :crop_data (generate-string (reduce (fn [build [key val]] (conj build {key (parse-long val)}))
                                                     {}
                                                     (map-of hpercent vpercent hoffset voffset)))})
  (queuefunc/recompress image_id))
