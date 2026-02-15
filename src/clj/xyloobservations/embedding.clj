(ns xyloobservations.embedding
  "Functions for interacting with the SigLIP embedding API"
  (:require
   [xyloobservations.config :refer [env]]
   [clj-http.client :as httpclient]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]))

(defn- format-embedding
  "Format embedding vector as string for pgvector: [0.1,0.2,0.3,...]"
  [embedding]
  (when embedding
    (str "[" (clojure.string/join "," embedding) "]")))

(defn generate-text-embedding
  "Generate embedding from text using the SigLIP API"
  [text]
  (try
    (let [siglip-api-url (str (env :siglip-api-url "http://localhost:8000/embed") "/text")
          response (httpclient/post siglip-api-url
                                   {:form-params {:text text}
                                    :content-type :json
                                    :as :json})
          embedding (get-in response [:body :embedding])]
      (format-embedding embedding))
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
          embedding (get-in response [:body :embedding])]
      (format-embedding embedding))
    (catch Exception e
      (log/error (format "Failed to generate image embedding: %s" e))
      nil)))
