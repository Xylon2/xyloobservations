(ns xyloobservations.embedding
  "Functions for interacting with the SigLIP embedding API"
  (:require
   [xyloobservations.config :refer [env]]
   [xyloobservations.db.core :as db]
   [clj-http.client :as httpclient]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]))

(defn- format-embedding
  "Format embedding vector as string for pgvector: [0.1,0.2,0.3,...]"
  [embedding]
  (when embedding
    (str "[" (clojure.string/join "," embedding) "]")))

(defn- decode-binary-embedding
  "Decode base64-encoded binary float array (32-bit floats) to vector"
  [base64-str]
  (when base64-str
    (let [decoder (java.util.Base64/getDecoder)
          bytes (.decode decoder base64-str)
          buffer (java.nio.ByteBuffer/wrap bytes)
          _ (.order buffer java.nio.ByteOrder/LITTLE_ENDIAN)
          float-count (/ (count bytes) 4)]
      (vec (for [_ (range float-count)]
             (.getFloat buffer))))))

(defn- generate-text-embedding-from-api
  "Generate embedding from text by calling the SigLIP API"
  [text]
  (let [siglip-api-url (str (env :siglip-api-url "http://localhost:8000/embed") "/text")
        response (httpclient/post siglip-api-url
                                 {:form-params {:text text}
                                  :content-type :json
                                  :as :json})
        base64-embedding (get-in response [:body :embedding])
        embedding (decode-binary-embedding base64-embedding)]
    (format-embedding embedding)))

(defn generate-text-embedding
  "Generate embedding from text using the SigLIP API, with caching"
  [text]
  (try
    ;; Check cache first
    (if-let [cached (db/get-cached-text-embedding {:text text})]
      (do
        (log/info (format "Cache hit for text: %s" text))
        (:embedding cached))
      ;; Cache miss - call API and cache result
      (do
        (log/info (format "Cache miss for text: %s" text))
        (when-let [embedding-str (generate-text-embedding-from-api text)]
          ;; Cache the result for future use
          (db/cache-text-embedding! {:text text :embedding embedding-str})
          embedding-str)))
    (catch Exception e
      (log/error (format "Failed to generate text embedding: %s" e))
      nil)))

(defn generate-image-embedding
  "Generate embedding from image file using the SigLIP API"
  [image-file-path]
  (try
    (let [siglip-api-url (str (env :siglip-api-url "http://localhost:8000/embed") "/image/upload")
          response (httpclient/post siglip-api-url
                                   {:multipart [{:name "file"
                                                :content (io/file image-file-path)}]
                                    :as :json})
          base64-embedding (get-in response [:body :embedding])
          embedding (decode-binary-embedding base64-embedding)]
      (format-embedding embedding))
    (catch Exception e
      (log/error (format "Failed to generate image embedding: %s" e))
      nil)))

(defn clear-cache!
  "Clear all cached text embeddings"
  []
  (db/clear-text-embedding-cache!)
  (log/info "Cleared text embedding cache"))

(defn cleanup-old-cache!
  "Delete cached text embeddings older than the specified age (e.g., '30 days')"
  [age]
  (let [result (db/cleanup-old-text-embeddings! {:age age})]
    (log/info (format "Cleaned up %s old text embeddings (age: %s)" result age))
    result))
