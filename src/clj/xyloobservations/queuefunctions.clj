(ns xyloobservations.queuefunctions
  (:gen-class)
  (:use
   [amazonica.aws.s3])
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [xyloobservations.config :refer [env]]
            [taoensso.nippy :as nippy]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [xyloobservations.db.core :as db]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(def ^{:const true}
  default-exchange-name "")

(defn awscreds
  "makes a map of aws creds in the format put-object likes"
  []
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)
   :endpoint (env :aws-region)})

(defn message-handler
  [ch meta ^bytes payload]
  (let [message (nippy/thaw payload)
        {:keys [image_id object_ref mimetype imagebytes]} message]
    (log/info (format "received image id %s with ref %s"
                      image_id
                      object_ref))
    (db/update-progress! {:image_id image_id :progress "resizing"})
    (Thread/sleep 6000)
    (db/update-progress! {:image_id image_id :progress "saving"})
    (put-object (awscreds)
                :bucket-name (env :bucket-name)
                :key object_ref
                :metadata {:content-type mimetype
                           :cache-control "public, max-age=31536000, immutable"}
                :input-stream (java.io.ByteArrayInputStream. imagebytes))
    (Thread/sleep 6000)
    (log/info (format "uploaded image id %s with ref %s"
                      image_id
                      object_ref))
    (db/update-progress! {:image_id image_id :progress "complete"})))

(mount/defstate thequeue
  :start (let [conn (rmq/connect)
               ch (lch/open conn)
               qname "xyloobservations.imagequeue"]
           (log/info "starting the queue")
           (lq/declare ch qname {:exclusive false :auto-delete true})
           (lc/subscribe ch qname message-handler {:auto-ack true})
           (map-of conn ch qname))
  :stop (let [{:keys [conn ch qname]} thequeue]
          (log/info "stopping the queue")
          (lch/close ch)
          (rmq/close conn)
          ))

(defn add [tempfile object_ref image_id mimetype]
  ;; need to pickle
  (let [imagebytes (slurp-bytes tempfile)]
    (lb/publish (thequeue :ch) default-exchange-name (thequeue :qname)
                (nippy/freeze (map-of imagebytes object_ref image_id mimetype))
                {:content-type "application/json" :type "new_image"})))
