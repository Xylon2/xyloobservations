(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn add-tag-page [request]
  (layout/render request "add_tag.html" {}))

(defn add-tag-submit [request]
  (layout/render request "add_tag.html" {}))

(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/add_tag" {:get add-tag-page
                :post add-tag-submit}]])
