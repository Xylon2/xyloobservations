(ns xyloobservations.homefunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]
   [clojure.java.io :as io]))

(defn get-orphaned-images []
  (db/orphan-images))

(defn fetch-image [image_id]
  (-> {:image_id image_id}
      db/fetch-image
      :imagedata
      io/input-stream))
