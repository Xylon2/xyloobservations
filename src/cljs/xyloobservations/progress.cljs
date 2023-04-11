(ns xyloobservations.progress
  (:require [ajax.core :as ajax]
            [reagent.core :as r]
            [reagent.dom  :as dom]
            [clormat.core :refer [format]]))

(def progresstype (.-value (.getElementById js/document "progresstype")))
(def ajform (.getElementById js/document "ajaxform"))
(def sbmtbtn  (.getElementById js/document "ajaxsubmit"))
(def image_id  (.-value (.getElementById js/document "image_id")))

(defn log
  "concatenate and print to console"
  [& strings]
  ((.-log js/console) (reduce str strings)))

(defn set-message
  ""
  [{:keys [msgtype msgtxt]}]
  (let [msgspan  (.getElementById js/document "message")]
    (set! (.. msgspan -style -color)
          (case msgtype
            "info" "blue"
            "success" "green"
            "red"))
    (when (some? msgtxt)
    (set! (.. msgspan -textContent) msgtxt))))

(defn loadimage
  ""
  []
  (ajax/GET
   (str "/image_deets_ajax?id=" image_id)
   {:handler
    (fn [{prefix :full_prefix
          {:keys [tiny small medium]} :sizes}]
      (dom/render
       [:img#theimage
        {:srcSet (format
                  "%s_tiny.%s %sw, %s_small.%s %sw, %s_medium.%s %sw"
                  prefix (tiny :extension) (tiny :width)
                  prefix (small :extension) (small :width)
                  prefix (medium :extension) (medium :width))
         :sizes (format
                 "(max-width: 640px) %spx, (max-width: 960px) %spx, %spx"
                 (tiny :width)
                 (small :width)
                 (medium :width))
         :src (format
               "%s_tiny.%s"
               prefix (tiny :extension))}]
       (.getElementById js/document "imgwrap")))}))

(defn pollhandler
  ""
  [{msgtype :msgtype :as response}]
  (set-message response)
  (if (#{"success" "error"} msgtype)
    ;; our job is done
    (do
      (set! (.-disabled sbmtbtn) false)
      (when (= progresstype "crop")
        ;; we need to reload the image, which means re-writing it's srcset, sizes and src
        (comment "todo")))
    (js/setTimeout dopoll 1000)))

(defn dopoll
  ""
  []
  (ajax/GET
   (str "/image_progress?image_id=" image_id)
   {:handler
    #(pollhandler %)}))

(defn error-handler
  ""
  [])

(defn success-handler
  ""
  [response]
  (set-message response)
  (when-not (= (response :msgtype) "error")
    (when (= progresstype "upload")
      (.trigger ajform "reset"))
    (dopoll)))

(defn handle-form
  ""
  [event]
  (let [actionurl (.. event -currentTarget -action)
        newimage (.getElementById js/document "dynamiccontent")
        msgspan  (.getElementById js/document "message")]

    ;; check they specified a file
    (when (= progresstype "upload")
      (when (= (.-value newimage) "")
        (set! (.. msgspan -style -color) "red")
        (set! (.. msgspan -textContent) "don't forget to choose a file")))

    ;; disable the form. update the message
    (set! (.-disabled sbmtbtn) true)
    (set! (.. msgspan -style -color) "blue")
    (set! (.. msgspan -textContent) (case progresstype
                                      "upload" "uploading......."
                                      "crop"   "pending........."))

    (let [formdata (js/FormData. ajform)]
      (ajax/POST
       actionurl
       {:body formdata
        :handler success-handler
        ;; :error-handler error-handler
        }))
    
))

(loadimage)

(.addEventListener ajform "submit"
                   (fn [event]
                     (.preventDefault event)
                     (handle-form event)))
