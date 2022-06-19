(ns xyloobservations.routes.home
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.db.core :as db]
   [xyloobservations.homefunctions :as homefunc]
   [clojure.java.io :as io]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn gallery [request]
  (let [tags (map #(Integer/parseInt %) (homefunc/always-vector ((request :query-params) "tags")))]
    (if (> (count tags) 0)
      (layout/render request "gallery.html" {:images (homefunc/matching-images tags)
                                             :filters (db/names-for-tags {:tags tags})
                                             :alltags (db/all-tags-with-images)
                                             :loggedin (contains? (request :session) :user)})
      (layout/render request "gallery.html" {:images (homefunc/images-with-tags)
                                             :alltags (db/all-tags-with-images)
                                             :loggedin (contains? (request :session) :user)}))))

(defn image [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        {:keys [imagedata mimetype]} (homefunc/fetch-image image_id)]
    (->  (response/ok imagedata)
         (response/content-type mimetype)
         (response/header "Cache-Control" "public, max-age=31536000, immutable"))))

(defn random [request]
  (let [numimages (Integer/parseInt (homefunc/default-number ((request :query-params) "num")))]
    (layout/render request "random.html" {:images (db/random-images {:numimages numimages})
                                          :numimages numimages
                                          :loggedin (contains? (request :session) :user)})))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get gallery}]
   ["/image" {:get image}]
   ["/random" {:get random}]])

