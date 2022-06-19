(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.middleware :as middleware]
   [xyloobservations.adminfunctions :as admin]
   [ring.util.response]
   [ring.util.http-response :as response]
   [xyloobservations.db.core :as db]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn add-tag-page [request]
  (layout/render request "add_tag.html" {:loggedin (contains? (request :session) :user)}))

(defn add-tag-submit [request]
  (let [{:keys [tagname description]} (request :params)]
    ;; we add it and show the form again so they
    ;; may add another
    (try (do (admin/add-tag! tagname description)
             (layout/render request "add_tag.html" {:msgtype "success"
                                                    :msgtxt "successfully added tag"
                                                    :loggedin (contains? (request :session) :user)}))
         (catch AssertionError e
           (layout/render request "add_tag.html" {:msgtype "error"
                                                  :msgtxt (str "validation error: " (.getMessage e))
                                                  :loggedin (contains? (request :session) :user)})))))

(defn upload-image-page [request]
  (layout/render request "upload_image.html" {:loggedin (contains? (request :session) :user)}))

(defn upload-image-submit [request]
  (let [file (request :multipart-params)
        caption (-> request :params :caption)]
    (try (do (admin/upload-image! file caption)
             (layout/render request "upload_image.html" {:msgtype "success"
                                                         :msgtxt "successfully uploaded image"
                                                         :loggedin (contains? (request :session) :user)}))
         (catch AssertionError e
           (layout/render request "upload_image.html" {:msgtype "error"
                                                       :msgtxt (str "validation error: " (.getMessage e))
                                                       :loggedin (contains? (request :session) :user)})))))

(defn image-settings-page [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        attached_tags (db/tag_names_of_image {:image_id image_id})
        caption ((db/get-caption {:image_id image_id}) :caption)
        all_tags (db/all_tags)
        loggedin (contains? (request :session) :user)]
    (layout/render request "image_settings.html" (map-of image_id attached_tags all_tags caption loggedin))))

(defn image-settings-submit [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        newtag (-> request :params :tag Integer/parseInt)
        dropdownsubmit (-> request :params :dropdownsubmit)
        newcaption (-> request :params :caption)
        loggedin (contains? (request :session) :user)]
    (if (= dropdownsubmit "true")
     (admin/tag-image! newtag, image_id)
      (admin/update-caption! newcaption, image_id))
    (let [attached_tags (db/tag_names_of_image {:image_id image_id})
          caption ((db/get-caption {:image_id image_id}) :caption)
          all_tags (db/all_tags)]
      (layout/render request "image_settings.html" (map-of image_id attached_tags all_tags caption loggedin)))))

(defn orphan_images [request]
  (layout/render request "orphan_images.html" {:orphans (db/orphan-images)
                                               :loggedin (contains? (request :session) :user)}))

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
