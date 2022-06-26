(ns xyloobservations.adminfunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn sanitize_advanced [thing]
  "our checkbox gives us either a null or a string \"true\".
   we need to convert that into true/false"
  (if (= "true" thing)
    true
    false)
  )

(defn add-tag! [tagname description advanced]
  (if (some empty? [tagname description])
    (throw (AssertionError. "empty values are not allowed")))
  (db/add-tag! (map-of tagname description advanced)))

(defn upload-image! [{{:keys [tempfile size filename]} "filename"}
                     caption
                     chozen_tags]
  ;; size is the filesize in bytes
  (let [mimetype (-> (str/split filename #"\.")
                     last
                     {"jpeg" "image/jpeg"
                      "jpg"  "image/jpeg"
                      "avif" "image/avif"
                      "webp" "image/webp"
                      "png"  "image/png"})
        tag_integers (map #(Integer/parseInt %) chozen_tags)]
    (when (not mimetype)
      (throw (AssertionError. "cannot detect file-type based on extension")))
    (when (> size 1000000)
      (throw (AssertionError. "this picture is too big")))
    (jdbc/with-transaction [t-conn db/*db*]
      (def image_id (:image_id (db/upload-image! t-conn
                                                 {:imagedata (slurp-bytes tempfile)
                                                  :mimetype mimetype
                                                  :caption caption})))
      (when-not (empty? tag_integers)
        (db/tag-image! t-conn {:taglist tag_integers
                               :image_id image_id})))))

(defn tag-image! [tag_id, image_id]
  "assign a tag to an image"
  (if (-> (db/find-imagetag (map-of tag_id image_id))
          :count
          (= 0)
          not)
    (throw (AssertionError. "this tag is already assigned to this image")))
  (db/tag-image! {:taglist [tag_id] :image_id image_id}))

(defn update-caption! [newcaption, image_id]
  "simply update the caption"
  (db/update-caption! (map-of newcaption image_id)))

