(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.middleware :as middleware]
   [xyloobservations.adminfunctions :as admin]
   [ring.util.response]
   [ring.util.http-response :as response]
   [xyloobservations.db.core :as db]))

(defn add-tag-page [request]
  (layout/render request "add_tag.html" {}))

(defn add-tag-submit [request]
  (let [{:keys [tagname description]} (request :params)]
    ;; we add it and show the form again so they
    ;; may add another
    (try (do (admin/add-tag! tagname description)
             (layout/render request "add_tag.html" {:msgtype "success" :msgtxt "successfully added tag"}))
         (catch AssertionError e (layout/render request "add_tag.html" {:msgtype "error" :msgtxt (str "validation error: " (.getMessage e))})))
    ))

(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/add_tag" {:get add-tag-page
                :post add-tag-submit}]])
