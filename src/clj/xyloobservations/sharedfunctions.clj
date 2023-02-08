(ns xyloobservations.sharedfunctions
  (:require
   [next.jdbc :as jdbc]
   [xyloobservations.layout :as layout]
   [xyloobservations.db.core :as db]))

(defn always-vector
  "this function takes something which may or may not be a vector and makes it always a vector"
  [item]
  (cond
    (= (type item) clojure.lang.PersistentVector) item
    (nil? item) []
    :else [item]))

(defn urlencode [foo]
  (java.net.URLEncoder/encode foo "UTF-8"))

(defn resolve_images
  "We output a sequence of maps, each containing an id, caption, a urlprefix and a map of sizes.
   The images argument gives us image_id, object_ref, url_prefix, caption and imagemeta."
  [images]
  (for [x images]
    {:image_id (x :image_id)
     :caption (x :caption)
     :full_prefix (str (x :url_prefix) (x :object_ref))
     :date  (let [{tag_name :tag_name :or {tag_name nil}} (db/fetch-date {:image_id (x :image_id)})] tag_name)
     :place (let [{tag_name :tag_name :or {tag_name nil}} (db/fetch-place {:image_id (x :image_id)})] tag_name)
     :sizes (x :imagemeta)}))

(defn myrender "simply a wrapper for layout/render to add commonly used arguments"
  [request template argmap]
  (layout/render request
                 template
                 (conj argmap {:loggedin (contains? (request :session) :user)
                               :fullpath (urlencode (str (request :path-info) "?" (request :query-string)))})))

