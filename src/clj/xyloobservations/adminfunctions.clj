(ns xyloobservations.adminfunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn add-tag! [tagname description]
  (if (some empty? [tagname description])
    (throw (AssertionError. "empty values are not allowed")))
  (jdbc/with-transaction [t-conn db/*db*]
    (db/add-tag! t-conn
                 {:tagname tagname
                  :description description})))

(defn upload-image! [{{:keys [tempfile size filename]} "filename"}
                     caption]
  ;; size is the filesize in bytes
  (let [mimetype (-> (str/split filename #"\.")
                     last
                     {"jpeg" "image/jpeg"
                      "jpg"  "image/jpeg"
                      "avif" "image/avif"
                      "webp" "image/webp"
                      "png"  "image/png"})]
    (when (not mimetype)
      (throw (AssertionError. "cannot detect file-type based on extension")))
    (when (> size 1000000)
      (throw (AssertionError. "this picture is too big")))
    (jdbc/with-transaction [t-conn db/*db*]
      (db/upload-image! t-conn
                        {:imagedata (slurp-bytes tempfile)
                         :mimetype mimetype
                         :caption caption}))))

(defn tag-image! [tag_id, image_id]
  "simply assigning a tag to an image"
  (db/tag-image! {:tag_id tag_id
                  :image_id image_id}))
