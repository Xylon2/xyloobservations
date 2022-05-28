(ns xyloobservations.adminfunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]))

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

(defn upload-image! [{{:keys [tempfile size filename]} "filename"}]
  ;; size is the filesize in bytes
  (if (> size 1000000)
    (throw (AssertionError. "this picture is too big")))
  (jdbc/with-transaction [t-conn db/*db*]
    (db/upload-image! t-conn
                 {:imagedata (slurp-bytes tempfile)})))