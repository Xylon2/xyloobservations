(ns xyloobservations.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[xyloobservations started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[xyloobservations has shut down successfully]=-"))
   :middleware identity})
