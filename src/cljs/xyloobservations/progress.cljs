(ns xyloobservations.progress
  (:require [ajax.core :as ajax]
            [reagent.core :as r]
            [reagent.dom  :as dom]
            [clormat.core :refer [format]]))

(def progresstype (.-value (.getElementById js/document "progresstype")))
(def ajform       (.getElementById js/document "ajaxform"))
(def sbmtbtn      (.getElementById js/document "ajaxsubmit"))

(def imgdeets (r/atom {}))

;; because of two functions which both call each-other
(declare dopoll)

(defn log
  "concatenate and print to console"
  [& strings]
  ((.-log js/console) (reduce str strings)))

(defn set-message
  "update the message span"
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
  "get the details of the image and add to the atom"
  [image_id]
  (ajax/GET
   (str "/image_deets_ajax?id=" image_id)
   {:handler
    (fn [deets]
      (reset! imgdeets deets))}))

(defn pollhandler
  "check the progress, and either continue checking or bail"
  [image_id {msgtype :msgtype :as response}]
  (set-message response)
  (if (#{"success" "error"} msgtype)
    ;; our job is done
    (do
      (set! (.-disabled sbmtbtn) false)
      (when (= progresstype "crop")
        ;; we need to reload the image, which means re-writing it's srcset, sizes and src
        (loadimage image_id)))
    ;; poll again
    (js/setTimeout #(dopoll image_id) 1000)))

(defn dopoll
  "hit the image_progress endpoint and see what happens"
  [image_id]
  (ajax/GET
   (str "/image_progress?image_id=" image_id)
   {:handler #(pollhandler image_id %)
    :error-handler (fn [{:keys [status status-text]}]
                     (pollhandler image_id
                                  {:msgtype "error"
                                   :msgtxt (str "Error: " status " " status-text)}))}))

(defn error-handler
  "simply set error message"
  [{:keys [status status-text]}]
  (set-message {:msgtype "error"
                :msgtxt (str "Error: " status " " status-text)}))

(defn success-handler
  "in case of success, start polling"
  [{image_id :image_id :as response}]
  (set-message response)
  ;; just because AJAX succeeded doesn't mean backend did
  (when-not (= (response :msgtype) "error")
    (when (= progresstype "upload")
      (.reset ajform))
    (dopoll image_id)))

(defn handle-form
  "AJAX form submission"
  [event]
  (let [actionurl (.. event -currentTarget -action)
        newimage (.getElementById js/document "newimage")
        msgspan  (.getElementById js/document "message")]

    ;; check they specified a file
    (if (and (= progresstype "upload") (= (.-value newimage) ""))
      (do
        (set! (.. msgspan -style -color) "red")
        (set! (.. msgspan -textContent) "don't forget to choose a file"))
      (do
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
            :error-handler error-handler
            }))))))

(defn imgrender
  "called by reagent to render the image tag from the atom"
  []
  (let [{prefix :full_prefix
         {:keys [tiny small medium]} :sizes} @imgdeets]
    ;; this is to prevent an error where the atom is unset and it dies
    (if (nil? prefix)
      [:img]
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
              prefix (tiny :extension))}])))

(when
    (= progresstype "crop")
    (let [image_id (.-value (.getElementById js/document "image_id"))]
     (loadimage image_id)
     (dom/render
      [imgrender]
      (.getElementById js/document "imgwrap"))))

(.addEventListener ajform "submit"
                   (fn [event]
                     (.preventDefault event)
                     (handle-form event)))
