(ns xyloobservations.progress
  (:require [ajax.core :as ajax]))

(defn log
  "concatenate and print to console"
  [& strings]
  ((.-log js/console) (reduce str strings)))

(defn error-handler
  ""
  [])

(defn success-handler
  ""
  [response]
  (log "response: " response))

(defn handle-form
  ""
  [event form]
  (let [progresstype (.-value (.getElementById js/document "progresstype"))
        actionurl (.. event -currentTarget -action)
        newimage (.getElementById js/document "dynamiccontent")
        msgspan  (.getElementById js/document "message")
        sbmtbtn  (.getElementById js/document "ajaxsubmit")]

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

    (let [formdata (js/FormData. form)]
      (ajax/POST
       actionurl
       {:body formdata
        :handler success-handler
        ;; :error-handler error-handler
        }))
    
))

(let [form (.getElementById js/document "ajaxform")]
  (.addEventListener form "submit"
                     (fn [event]
                       (.preventDefault event)
                       (handle-form event form))))
