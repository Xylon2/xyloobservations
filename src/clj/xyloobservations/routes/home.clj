(ns xyloobservations.routes.home
  (:require
   [xyloobservations.db.core :as db]
   [xyloobservations.sharedfunctions :as shared]
   [xyloobservations.embedding :as embedding]
   [xyloobservations.middleware :as middleware]
   [clojure.tools.logging :as log]
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

(defn search [request]
  (let [{{query "q"} :query-params} request]
    (if query
      (if-let [embedding-str (embedding/generate-text-embedding query)]
        (let [results (db/search-by-embedding {:embedding embedding-str :limit 50})]
          (log/info (format "Found %d results for query: %s" (count results) query))
          (shared/myrender request "search.html" {:query query
                                                   :images (shared/resolve_images results)}))
        ;; If embedding generation failed, show error
        (do
          (log/error (format "Failed to generate embedding for query: %s" query))
          (shared/myrender request "search.html" {:query query
                                                   :error "Failed to generate embedding"})))
      (shared/myrender request "search.html" {}))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get #(gallery "gallery.html" %)}]
   ["/advanced" {:get #(gallery "advanced.html" %)}]
   ["/random" {:get random}]
   ["/search" {:get search}]
   ["/about" {:get about}]])

