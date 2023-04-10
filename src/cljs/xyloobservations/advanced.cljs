(ns xyloobservations.advanced)

(let [sbmt (.getElementById js/document "filtersubmit")]
    (.addEventListener sbmt "click"
                       #(set! (.-hash js/location) "thepictures")))

