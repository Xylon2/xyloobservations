(ns xyloobservations.routes.home
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.db.core :as db]
   [xyloobservations.homefunctions :as homefunc]
   [clojure.java.io :as io]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn myrender [request template argmap]
  "simply a wrapper for layout/render to add commonly used arguments"
  (layout/render request
                 template
                 (conj argmap {:loggedin (contains? (request :session) :user)
                               :fullpath (str (request :path-info) "?" (request :query-string))})))

(defn gallery [template request]
  (let [tags (map #(Integer/parseInt %) (homefunc/always-vector ((request :query-params) "tags")))]
    (if (> (count tags) 0)
      (myrender request template {:images (homefunc/matching-images tags)
                                  :filters (db/names-for-tags {:tags tags})
                                  :alltags (db/all-tags-with-images)})
      (myrender request template {:images (homefunc/images-with-tags)
                                  :alltags (db/all-tags-with-images)}))))

(defn image [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        {:keys [imagedata mimetype]} (homefunc/fetch-image image_id)]
    (->  (response/ok imagedata)
         (response/content-type mimetype)
         (response/header "Cache-Control" "public, max-age=31536000, immutable"))))

(defn random [request]
  (let [numimages (Integer/parseInt (homefunc/default-number ((request :query-params) "num")))]
    (myrender request "random.html" {:images (db/random-images {:numimages numimages})
                                     :numimages numimages})))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get #(gallery "gallery.html" %)}]
   ["/advanced" {:get #(gallery "advanced.html" %)}]
   ["/image" {:get image}]
   ["/random" {:get random}]])

