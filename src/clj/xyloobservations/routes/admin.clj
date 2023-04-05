(ns xyloobservations.routes.admin
  (:require
   [xyloobservations.middleware :as middleware]
   [xyloobservations.adminfunctions :as adminfunc]
   [xyloobservations.sharedfunctions :as shared]
   [ring.util.response]
   [ring.util.http-response :as response]
   [xyloobservations.db.core :as db]
   [cheshire.core :refer [generate-string parse-string]]
   [xyloobservations.queuefunctions :as queuefunc]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn log
  "concatenate and save to file"
  [& strings]
  (spit "/tmp/cljdebug.txt" (str (reduce str strings) "
")))

(defn tag-manager-page [request]
  (let [all_tags (db/all_tags)]
    (shared/myrender request "tag_manager.html" {:all_tags all_tags})))

(defn tag-manager-submit [request]
  (let [{{:keys [tagname description advanced]} :params} request]
    ;; we add it and show the form again so they may add another
    (try
      (do (adminfunc/add-tag! tagname description advanced)
          (def message {:msgtype "success"
                        :msgtxt "successfully added tag"}))
      (catch AssertionError e
        (def message {:msgtype "error"
                      :msgtxt (str "validation error: " (.getMessage e))})))
    (let [all_tags (db/all_tags)]
      (shared/myrender request "tag_manager.html" (conj {:all_tags all_tags} message)))))

(defn upload-image-page [request]
  (let [all_tags (db/all_tags)]
    (shared/myrender request "upload_image.html" {:all_tags all_tags :progresstype "upload"})))

(defn upload-image-ajax [{file :multipart-params
                          {caption :caption
                           chozen_tags :tags} :params}]
  (response/ok
   (try (let [image_id (adminfunc/upload-image! file caption chozen_tags)]
          {:msgtype "info" :msgtxt "...queued........." :image_id image_id})
        (catch AssertionError e
          {:msgtype "error" :msgtxt (str "validation error: " (.getMessage e))})
        (catch com.rabbitmq.client.AlreadyClosedException e
          {:msgtype "error" :msgtxt (str "failed adding image to queue: " (.getMessage e))}))))

(defn crop-image-ajax "update the image crop setting, and trigger the pipeline to re-compress the image"
  [{{image_id "id"} :query-params
    {:keys [hpercent vpercent hoffset voffset]} :params}]
  (response/ok
   (try
     (db/set-crop! {:image_id image_id
                    :crop_data (generate-string (reduce (fn [build [key val]] (conj build {key (parse-long val)}))
                                                        {}
                                                        (map-of hpercent vpercent hoffset voffset)))})
     (queuefunc/recompress image_id)
     {:msgtype "info" :msgtxt "...queued........." :image_id image_id}
     (catch AssertionError e
       {:msgtype "error" :msgtxt (str "validation error: " (.getMessage e))})
     (catch com.rabbitmq.client.AlreadyClosedException e
       {:msgtype "error" :msgtxt (str "failed adding image to queue: " (.getMessage e))}))))

(defn image-deets-ajax
  "Given an image id in the query-string, returns the full image deets.
   This is called after the image resize is complete, to update the
   image_settings page to show the new image."
  [{{image_id "id"} :query-params}]
  (response/ok
   (first
    (shared/resolve_images (db/caption-and-object {:image_id image_id})))))

(defn image-progress [request]
  (let [{{:strs [image_id]} :query-params} request
        {progress :progress} (db/get-progress {:image_id image_id})
        progress_styled (or ({"resizing" ".....resizing.....",
                              "saving"   ".........saving...",
                              "complete" "..........complete"} progress) progress)
        msgtype (case progress
                  "complete" "success"
                  "failed resizing" "error"
                  "failed saving" "error"
                  "info")]

    (response/ok {:msgtype msgtype :msgtxt progress_styled :image_id image_id})))

(defn image-settings-page [request]
  (let [{{image_id "id"
          redirect "redirect"} :query-params} request
        redirect (shared/urlencode redirect)
        attached_tags (db/tag_names_of_image {:image_id image_id})
        image (first (shared/resolve_images (db/caption-and-object {:image_id image_id})))
        all_tags (db/all_tags)
        crop_data ((db/get-crop-settings {:image_id image_id}) :crop_data)
        progresstype "crop"]
    (shared/myrender request "image_settings.html" (map-of image image_id attached_tags all_tags redirect crop_data progresstype))))

(defn image-settings-submit [{{image_id "id"
                               redirect "redirect"} :query-params
                              {whichform :whichform} :params :as request}]
  (let [redirect (shared/urlencode redirect)]
    (case whichform
      "add_tag" (adminfunc/tag-image! image_id request)
      "remove_tag" (adminfunc/untag-image! image_id request)
      "edit_caption" (adminfunc/update-caption! image_id request))
    (let [attached_tags (db/tag_names_of_image {:image_id image_id})
          image (first (shared/resolve_images (db/caption-and-object {:image_id image_id})))
          all_tags (db/all_tags)
          crop_data ((db/get-crop-settings {:image_id image_id}) :crop_data)
          progresstype "crop"]
      (shared/myrender request "image_settings.html" (map-of image image_id attached_tags all_tags redirect crop_data progresstype)))))

(defn orphan_images [request]
  (let [images (shared/resolve_images (db/orphan-images))]
    (shared/myrender request "orphan_images.html" {:orphans images})))

(defn confirm_delete_image [request]
  (let [{{image_id "id"
          redirect "redirect"} :query-params} request
        [image] (shared/resolve_images (db/caption-and-object {:image_id image_id}))]
    (shared/myrender request "delete_image.html" (map-of image image_id redirect))))

(defn delete_image [request]
  (let [{{image_id "id"
          redirect "redirect"} :query-params} request]
    (db/delete-image! {:image_id image_id})
    (response/found (if (empty? redirect) "/" redirect))))

(defn confirm_delete_tag [request]
  (let [{{tag_id "tag"
          redirect "redirect"} :query-params} request
        redirect (shared/urlencode redirect)
        images (db/images-by-tag {:tag_ref tag_id})
        tag_name (:tag_name (db/tag-info {:tag_id tag_id :cols ["tag_name"]}))]
    (shared/myrender request "delete_tag.html" (map-of tag_id redirect images tag_name))))

(defn delete_tag [request]
  (let [{{tag_id "tag"
          redirect "redirect"} :query-params} request]
    (db/delete-tag! (map-of tag_id))
    (response/found (if (empty? redirect) "/" redirect))))

(defn tag_settings_page [request]
  (let [{{tag_id "tag"
          redirect "redirect"} :query-params} request
        redirect (shared/urlencode redirect)
        {:keys [tag_name description advanced]} (db/tag-info {:tag_id tag_id :cols ["tag_name", "description", "advanced"]})
        ad_opts ["false" "date" "place"]]
    (shared/myrender request "tag_settings.html" (map-of tag_id redirect tag_name description advanced ad_opts))))

(defn tag_settings_submit [request]
  (let [{{tag_id "tag"
          redirect "redirect"} :query-params} request
        redirect (shared/urlencode redirect)
        {:keys [tag_name description advanced]} (request :params)]
    (adminfunc/modify_tag tag_id tag_name description advanced)
    (let [{:keys [tag_name description advanced]} (db/tag-info {:tag_id tag_id :cols ["tag_name", "description", "advanced"]})
          ad_opts ["false" "date" "place"]]
      (shared/myrender request "tag_settings.html" (map-of tag_id redirect tag_name description advanced ad_opts)))))

(defn nogetplz [request] (-> (response/ok "GET not accepted here")
                             (response/content-type "text/html")))

(defn admin-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-auth]}
   ["/tag_manager" {:get tag-manager-page
                    :post tag-manager-submit}]
   ["/upload_image" {:get upload-image-page}]
   ["/upload_image_ajax" {:get nogetplz
                          :post upload-image-ajax}]
   ["/crop_image_ajax" {:get nogetplz
                        :post crop-image-ajax}]
   ["/image_deets_ajax" {:get image-deets-ajax}]
   ["/image_progress" {:get image-progress}]
   ["/image_settings" {:get image-settings-page
                       :post image-settings-submit}]
   ["/orphan_images" {:get orphan_images}]
   ["/deleteimg" {:get confirm_delete_image
                  :post delete_image}]
   ["/delete_tag" {:get confirm_delete_tag
                   :post delete_tag}]
   ["/tag_settings" {:get tag_settings_page
                     :post tag_settings_submit}]])
