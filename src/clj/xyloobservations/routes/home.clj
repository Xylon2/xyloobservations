(ns xyloobservations.routes.home
  (:require
   [xyloobservations.db.core :as db]
   [xyloobservations.sharedfunctions :as shared]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]))

(defn images-with-tags "any images that have tags attached.
  output is image_id, object_ref, caption and imagemeta"
  []
  (distinct (db/images-with-tags)))

(defn matching-images
   "get a list of images that have all of a list of tags.
    output is image_id, object_ref, caption and imagemeta"
  [tags]
  (db/images-multi-tags {:tags (vec (map parse-long tags))}))

(defn default-number
  "if it's null, returns string 10"
  [item]
  (cond
    (= (type item) java.lang.String) item
    (nil? item) "10"))

(defn gallery [template request]
  (let [{{tags "tags"} :query-params} request
        tags' (shared/always-vector tags)]
    (if-not (empty? tags')
      (shared/myrender request template {:images (shared/resolve_images (matching-images tags'))
                                  :filters (db/names-for-tags {:tags tags'})
                                  :alltags (db/all-tags-with-images)})
      (shared/myrender request template {:images (shared/resolve_images (images-with-tags))
                                  :alltags (db/all-tags-with-images)}))))

(defn random [request]
  (let [{{numimages "num"} :query-params} request
        numimages' (default-number numimages)]
    (shared/myrender request "random.html" {:images (shared/resolve_images (db/random-images {:numimages numimages'}))
                                     :numimages numimages'})))

(defn about [request]
  (shared/myrender request "about.html" {}))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get #(gallery "gallery.html" %)}]
   ["/advanced" {:get #(gallery "advanced.html" %)}]
   ["/random" {:get random}]
   ["/about" {:get about}]])

