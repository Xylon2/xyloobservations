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
            [xyloobservations.db.core :as db]
            [xyloobservations.resizingfunctions :as resizers]
            [cheshire.core :refer :all]
            [xyloobservations.mimetypes :as mimetypes]
            [clj-http.client :as httpclient]))

(defmacro map-of
  [& xs]
  `(hash-map ~@(mapcat (juxt keyword identity) xs)))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(def ^{:const true}
  default-exchange-name "")

(defn img-id-gen []
  (str (.toString (java.util.UUID/randomUUID))))

(defn awscreds
  "makes a map of aws creds in the format put-object likes"
  []
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)
   :endpoint (env :aws-region)})

(def amqp-url (get (System/getenv) "CLOUDAMQP_URL" "amqp://guest:guest@localhost:5672"))

(defn upload-to-s3
  "takes a vector of maps of files to upload"
  [object_ref files]
  (doseq [file files]
    (let [{:keys [filepath mimetype identifier width height]} file]
      (put-object (awscreds)
                  :bucket-name (env :bucket-name)
                  :key (str object_ref "_" identifier "." (mimetypes/type-to-extension mimetype))
                  :metadata {:content-type mimetype
                             :cache-control "public, max-age=31536000, immutable"}
                  ;; :input-stream (java.io.ByteArrayInputStream. imagebytes)
                  :file (io/file filepath)))))

(defn save-to-filesystem
  "takes a vector of maps of files to save"
  [object_ref files]
  (doseq [file files]
    (let [{:keys [filepath mimetype identifier width height]} file]
      (copy-file filepath (str (env :img-path) object_ref "_" identifier "." (mimetypes/type-to-extension mimetype)))
      )))

(defn cleanup-files
  "takes a vector of maps of files to delete"
  [files]
  (doseq [file files]
    (let [{:keys [filepath]} file]
      (io/delete-file filepath))))

(defn extract-key [buildme innermap]
  (conj buildme {(keyword (innermap :identifier)) (dissoc innermap :filepath :identifier)}))

(defn update-and-save
  "upload the items from the uploadme var and save the metadata"
  [image_id object_ref uploadme]
  (case (env :image-store)
    "s3"
    (upload-to-s3 object_ref uploadme)
    "filesystem"
    (save-to-filesystem object_ref uploadme))
  (db/save-meta! {:imagemeta (generate-string (reduce extract-key {} uploadme))
                  :image_id image_id
                  :object_ref object_ref}))

(defn message-handler
  [ch {:keys [type]} ^bytes payload]
  (let [message (nippy/thaw payload)
        {:keys [image_id mimetype size imagebytes]} message
        object_ref (img-id-gen)]
    (log/info (format "%s: received image id %s and chozen ref %s" type image_id object_ref))
    (db/update-progress! {:image_id image_id :progress "resizing"})

    ;; resize
    (try
      (def uploadme (resizers/resize size imagebytes image_id mimetype))
      (log/info (format "%s: resized image %s successfully" type image_id))
      (db/update-progress! {:image_id image_id :progress "saving"})
      (catch Exception e
        ;; we log the output of the exception, then we throw it again
        ;; to stop any further execution
        (log/info (format "%s: failed resizing image %s with exception: %s" type image_id e))
        (db/update-progress! {:image_id image_id :progress "failed resizing"})
        (throw (ex-info e {:type :resize-exception}))))

    ;; upload the items from the "uploadme" var and save the metadata
    (try
      (update-and-save image_id object_ref uploadme)
      (log/info (format "%s: uploaded image id %s with ref %s" type image_id object_ref))
      (db/update-progress! {:image_id image_id :progress "complete"})
      (catch Exception e
        ;; we log the output of the exception, then we throw it again
        ;; to stop any further execution
        (log/info (format "%s: failed saving image %s with exception: %s" type image_id e))
        (db/update-progress! {:image_id image_id :progress "failed saving"})
        (throw (ex-info e {:type :save-exception}))))

    ;; delete the temporary files resizers/resize made earlier
    (cleanup-files uploadme)))

(mount/defstate thequeue
  :start (let [conn (rmq/connect {:uri amqp-url})
               ch (lch/open conn)
               qname "xyloobservations.imagequeue"]
           (log/info "starting the queue")
           (lq/declare ch qname {:exclusive false :auto-delete true})
           (lc/subscribe ch qname message-handler {:auto-ack true})
           (map-of conn ch qname))
  :stop (let [{:keys [conn ch qname]} thequeue]
          (log/info "stopping the queue")
          (lch/close ch)
          (rmq/close conn)))

(defn add [tempfile image_id mimetype size]
  ;; need to pickle
  (let [imagebytes (slurp-bytes tempfile)]
    (lb/publish (thequeue :ch) default-exchange-name (thequeue :qname)
                (nippy/freeze (map-of imagebytes image_id mimetype size))
                {:content-type "application/json" :type "new_image"})))

(defn recompress [image_id]
  ;; need to download original image
  (let [{url-prefix :url-prefix} env
        [{{{:keys [extension mimetype]} :original} :imagemeta
          object_ref_old :object_ref}] (db/caption-and-object {:image_id image_id})
        old-img-url (str url-prefix object_ref_old "_original." extension)
        {imagebytes :body size :length} (httpclient/get old-img-url {:as :byte-array})]
    (lb/publish (thequeue :ch) default-exchange-name (thequeue :qname)
                (nippy/freeze (map-of imagebytes image_id mimetype size))
                {:content-type "application/json" :type "resize_image"})))
