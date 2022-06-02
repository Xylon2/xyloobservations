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

(defn upload-image-page [request]
  (layout/render request "upload_image.html"))

(defn upload-image-submit [request]
  (let [file (request :multipart-params)]
    (try (do (admin/upload-image! file)
             (layout/render request "upload_image.html" {:msgtype "success" :msgtxt "successfully uploaded image"}))
         (catch AssertionError e (layout/render request "upload_image.html" {:msgtype "error" :msgtxt (str "validation error: " (.getMessage e))})))))

(defn image-settings-page [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        attached_tags (db/tag_names_of_image {:image_id image_id})
        all_tags (db/all_tags)]
    (layout/render request "image_settings.html" {:image_id image_id
                                                  :attached_tags attached_tags
                                                  :all_tags all_tags})))


(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/add_tag" {:get add-tag-page
                :post add-tag-submit}]
   ["/upload_image" {:get upload-image-page
                     :post upload-image-submit}]
   ["/image_settings" {:get image-settings-page}]])
