(ns xyloobservations.mimetypes
  (:require [clojure.set :as cljset]))

(def extension-to-type
  {"jpeg" "image/jpeg"
   "JPG"  "image/jpeg"
   "jpg"  "image/jpeg"
   "avif" "image/avif"
   "webp" "image/webp"
   "png"  "image/png"})

(def type-to-extension
  ;; n.b. map-invert automatically de-duplicates keys so re-ordering
  ;; extension-to-type could affect our result here
  (cljset/map-invert extension-to-type))
