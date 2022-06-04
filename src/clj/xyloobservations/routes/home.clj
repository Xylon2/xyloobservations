(ns xyloobservations.routes.home
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.db.core :as db]
   [xyloobservations.homefunctions :as homefunc]
   [clojure.java.io :as io]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html" {:tags    (homefunc/tags-with-images)
                                      :orphans (homefunc/get-orphan-images)
                                      :loggedin (contains? (request :session) :user)}))

(defn image [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        {:keys [imagedata mimetype]} (homefunc/fetch-image image_id)]
    (->  (response/ok imagedata)
         (response/content-type mimetype)
         (response/header "Cache-Control" "public, max-age=31536000, immutable"))))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/image" {:get image}]
   ["/about" {:get about-page}]])

