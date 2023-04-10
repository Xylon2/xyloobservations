(ns xyloobservations.progress
  (:require [ajax.core :as ajax]))

(def progresstype (.-value (.getElementById js/document "progresstype")))
(def ajform (.getElementById js/document "ajaxform"))
(def sbmtbtn  (.getElementById js/document "ajaxsubmit"))

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

(defn pollhandler
  ""
  [image_id {msgtype :msgtype :as response}]
  (set-message response)
  (if (#{"success" "error"} msgtype)
    ;; our job is done
    (do
      (set! (.-disabled sbmtbtn) false)
      (when (= progresstype "crop")
        ;; we need to reload the image, which means re-writing it's srcset, sizes and src
        (comment "todo")))
    (js/setTimeout #(dopoll image_id) 1000)))

(defn dopoll
  ""
  [image_id]
  (ajax/GET
   (str "/image_progress?image_id=" image_id)
   {:handler
    #(pollhandler image_id %)}))

(defn error-handler
  ""
  [])

(defn success-handler
  ""
  [{image_id :image_id :as response}]
  (set-message response)
  (when-not (= (response :msgtype) "error")
    (when (= progresstype "upload")
      (.trigger ajform "reset"))
    (dopoll image_id)))

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

(.addEventListener ajform "submit"
                   (fn [event]
                     (.preventDefault event)
                     (handle-form event)))
