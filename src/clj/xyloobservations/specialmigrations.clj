(ns xyloobservations.specialmigrations
  (:require [xyloobservations.config :refer [env]]
            [clojure.tools.logging :as log]
            [xyloobservations.db.core :as db]
            [cheshire.core :refer :all]))

(defn set-url-prefix []
  (db/set-url-prefix! {:url_prefix (env :url-prefix)}))
