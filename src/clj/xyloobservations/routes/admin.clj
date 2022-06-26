(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.layout :as layout]
   [xyloobservations.middleware :as middleware]
   [xyloobservations.adminfunctions :as adminfunc]
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

(defn tag-manager-page [request]
  (myrender request "tag_manager.html" {}))

(defn tag-manager-submit [request]
  (let [{:keys [tagname description advanced]} (request :params)]
    ;; we add it and show the form again so they
    ;; may add another
    (try (do (adminfunc/add-tag! tagname description advanced)
             (myrender request "tag_manager.html" {:msgtype "success"
                                               :msgtxt "successfully added tag"}))
         (catch AssertionError e
           (myrender request "tag_manager.html" {:msgtype "error"
                                             :msgtxt (str "validation error: " (.getMessage e))})))))

(defn upload-image-page [request]
  (let [all_tags (db/all_tags)]
    (myrender request "upload_image.html" {:all_tags all_tags})))

(defn upload-image-submit [request]
  (let [file (request :multipart-params)
        caption (-> request :params :caption)
        chozen_tags (-> request :params :tags)
        all_tags (db/all_tags)]
    (try (do (adminfunc/upload-image! file caption chozen_tags)
             (myrender request "upload_image.html" {:all_tags all_tags
                                                    :msgtype "success"
                                                    :msgtxt "successfully uploaded image"}))
         (catch AssertionError e
           (myrender request "upload_image.html" {:all_tags all_tags
                                                  :msgtype "error"
                                                  :msgtxt (str "validation error: " (.getMessage e))})))))

(defn image-settings-page [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        attached_tags (db/tag_names_of_image {:image_id image_id})
        caption ((db/get-caption {:image_id image_id}) :caption)
        all_tags (db/all_tags)
        redirect ((request :query-params) "redirect")]
    (myrender request "image_settings.html" (map-of image_id attached_tags all_tags caption redirect))))

(defn image-settings-submit [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        newtag (-> request :params :tag Integer/parseInt)
        dropdownsubmit (-> request :params :dropdownsubmit)
        newcaption (-> request :params :caption)
        redirect ((request :query-params) "redirect")]
    (if (= dropdownsubmit "true")
     (adminfunc/tag-image! newtag, image_id)
      (adminfunc/update-caption! newcaption, image_id))
    (let [attached_tags (db/tag_names_of_image {:image_id image_id})
          caption ((db/get-caption {:image_id image_id}) :caption)
          all_tags (db/all_tags)]
      (myrender request "image_settings.html" (map-of image_id attached_tags all_tags caption redirect)))))

(defn orphan_images [request]
  (myrender request "orphan_images.html" {:orphans (db/orphan-images)}))

(defn confirm_delete [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        redirect ((request :query-params) "redirect")]
    (myrender request "delete_image.html" (map-of image_id redirect))))

(defn delete_image [request]
  (let [image_id (Integer/parseInt ((request :query-params) "id"))
        redirect ((request :query-params) "redirect")]
    (db/delete-image! {:image_id image_id})
    (response/found (if (empty? redirect) "/" redirect))))

(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/tag_manager" {:get tag-manager-page
                :post tag-manager-submit}]
   ["/upload_image" {:get upload-image-page
                     :post upload-image-submit}]
   ["/image_settings" {:get image-settings-page
                       :post image-settings-submit}]
   ["/orphan_images" {:get orphan_images}]
   ["/deleteimg" {:get confirm_delete
                  :post delete_image}]])
