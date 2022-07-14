(ns xyloobservations.routes.home
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.db.core :as db]
   [xyloobservations.homefunctions :as homefunc]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [xyloobservations.imagestorefuncs :as imgstore]))

(defn urlencode [foo]
  (java.net.URLEncoder/encode foo "UTF-8"))

(defn myrender [request template argmap]
  "simply a wrapper for layout/render to add commonly used arguments"
  (layout/render request
                 template
                 (conj argmap {:loggedin (contains? (request :session) :user)
                               :fullpath (urlencode (str (request :path-info) "?" (request :query-string)))})))

(defn gallery [template request]
  (let [tags (map #(Integer/parseInt %) (homefunc/always-vector ((request :query-params) "tags")))]
    (if-not (empty? tags)
      (myrender request template {:images (imgstore/resolve_images (homefunc/matching-images tags))
                                  :filters (db/names-for-tags {:tags tags})
                                  :alltags (db/all-tags-with-images)})
      (myrender request template {:images (imgstore/resolve_images (homefunc/images-with-tags))
                                  :alltags (db/all-tags-with-images)}))))

(defn random [request]
  (let [numimages (Integer/parseInt (homefunc/default-number ((request :query-params) "num")))]
    (myrender request "random.html" {:images (imgstore/resolve_images (db/random-images {:numimages numimages}))
                                     :numimages numimages})))

(defn about [request]
  (myrender request "about.html" {}))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get #(gallery "gallery.html" %)}]
   ["/advanced" {:get #(gallery "advanced.html" %)}]
   ["/random" {:get random}]
   ["/about" {:get about}]])

