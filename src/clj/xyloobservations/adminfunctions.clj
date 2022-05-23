(ns xyloobservations.adminfunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]))

(defn add-tag! [tagname description]
  (if (some #(empty? %) [tagname description])
    (throw (Exception. "empty values are not allowed")))
  (jdbc/with-transaction [t-conn db/*db*]
    (db/add-tag! t-conn
                 {:tagname tagname
                  :description description})))
