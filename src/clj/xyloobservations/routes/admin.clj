(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.middleware :as middleware]
   [xyloobservations.adminfunctions :as admin]
   [ring.util.response]
   [ring.util.http-response :as response]
   [xyloobservations.db.core :as db]))

(defn myrender [request template argmap]
  "simply a wrapper for layout/render to add commonly used arguments"
  (layout/render request
                 template
                 (conj argmap {:loggedin (contains? (request :session) :user)
                               :fullpath (str (request :path-info) "?" (request :query-string))})))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn add-tag-page [request]
  (myrender request "add_tag.html" {}))

(defn add-tag-submit [request]
  (let [{:keys [tagname description advanced]} (request :params)]
    ;; we add it and show the form again so they
    ;; may add another
    (try (do (admin/add-tag! tagname description advanced)
             (myrender request "add_tag.html" {:msgtype "success"
                                               :msgtxt "successfully added tag"}))
         (catch AssertionError e
           (myrender request "add_tag.html" {:msgtype "error"
                                             :msgtxt (str "validation error: " (.getMessage e))})))))

(defn upload-image-page [request]
  (myrender request "upload_image.html" {}))

(defn upload-image-submit [request]
  (let [file (request :multipart-params)
        caption (-> request :params :caption)]
    (try (do (admin/upload-image! file caption)
             (myrender request "upload_image.html" {:msgtype "success"
                                                    :msgtxt "successfully uploaded image"}))
         (catch AssertionError e
           (myrender request "upload_image.html" {:msgtype "error"
                                                  :msgtxt (str "validation error: " (.getMessage e))})))))

(defn image-settings-page [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        attached_tags (db/tag_names_of_image {:image_id image_id})
        caption ((db/get-caption {:image_id image_id}) :caption)
        all_tags (db/all_tags)]
    (myrender request "image_settings.html" (map-of image_id attached_tags all_tags caption))))

(defn image-settings-submit [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        newtag (-> request :params :tag Integer/parseInt)
        dropdownsubmit (-> request :params :dropdownsubmit)
        newcaption (-> request :params :caption)]
    (if (= dropdownsubmit "true")
     (admin/tag-image! newtag, image_id)
      (admin/update-caption! newcaption, image_id))
    (let [attached_tags (db/tag_names_of_image {:image_id image_id})
          caption ((db/get-caption {:image_id image_id}) :caption)
          all_tags (db/all_tags)]
      (myrender request "image_settings.html" (map-of image_id attached_tags all_tags caption)))))

(defn orphan_images [request]
  (myrender request "orphan_images.html" {:orphans (db/orphan-images)}))

(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/add_tag" {:get add-tag-page
                :post add-tag-submit}]
   ["/upload_image" {:get upload-image-page
                     :post upload-image-submit}]
   ["/image_settings" {:get image-settings-page
                       :post image-settings-submit}]
   ["/orphan_images" {:get orphan_images}]])
