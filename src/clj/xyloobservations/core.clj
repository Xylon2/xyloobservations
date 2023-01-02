(ns xyloobservations.core
  (:require
    [xyloobservations.handler :as handler]
    [xyloobservations.nrepl :as nrepl]
    [luminus.http-server :as http]
    [luminus-migrations.core :as migrations]
    [xyloobservations.config :refer [env]]
    [xyloobservations.authfunctions :as authfunc]
    [xyloobservations.specialmigrations :as specmig]
    [xyloobservations.queuefunctions :as queue]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.tools.logging :as log]
    [mount.core :as mount])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error {:what :uncaught-exception
                  :exception ex
                  :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
    (-> env
        (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime))))) 
        (assoc  :handler (handler/app))
        (update :port #(or (-> env :options :port) %))
        (select-keys [:handler :host :port])))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (migrations/migrate ["migrate"] (select-keys env [:database-url]))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn read-user-pass []
  (let [console (System/console)
        username (.readLine     console "%s" (into-array ["Enter username: "]))
        password (.readPassword console "%s" (into-array ["Enter password: "]))]
    {:username (apply str username)
     :password (apply str password)}))

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (when (re-find #"^\d+$" s)
    (read-string s)))

(defn -main [& args]
  (-> args
                            (parse-opts cli-options)
                            (mount/start-with-args #'xyloobservations.config/env))
  (cond
    ;; sanity checks
    (nil? (:database-url env))
    (do
      (log/error "Database config not found, :database-url environment variable must be set")
      (System/exit 1))
    (nil? (:image-store env))
    (do
      (log/error "Image store config not found, :image-store environment variable must be set")
      (System/exit 1))
    (nil? (#{"s3" "filesytem"} (:image-store env)))
    (do
      (log/error "Image store config invalid, :image-store should be s3 or filesystem")
      (System/exit 1))
    (nil? (#{"webp" "avif" "jpeg"} (:img-format env)))
    (do
      (log/error "No valid config detected for image format, :img-format environment variable must be set")
      (System/exit 1))

    ;; command-line actions
    (some #{"init"} args)
    (do
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (migrations/migration? args)
    (do
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    (some #{"add-user"} args)
    (let [{:keys [username password]} (read-user-pass)]
      (mount/start #'xyloobservations.db.core/*db*)
      (authfunc/create-user! username password)
      (System/exit 0))
    (some #{"special-migrate"} args)
    (do
      (mount/start #'xyloobservations.db.core/*db*)
      (specmig/set-url-prefix)
      (System/exit 0))
    (some #{"recompress-img"} args)
    (let [image_id (last args)]
      (if (parse-number image_id)
        (do
          (mount/start #'xyloobservations.db.core/*db* #'xyloobservations.queuefunctions/thequeue)
          (queue/recompress (parse-number image_id))
          (System/exit 0))
        (do
          (println "last arg must be image_id to recompress")
          (System/exit 1))))
    (some #{"recompress-all"} args)
    (do
      (mount/start #'xyloobservations.db.core/*db* #'xyloobservations.queuefunctions/thequeue)
      (doall (map #(queue/recompress %) (specmig/get-all-images)))
      (System/exit 0))
    :else
    (start-app args)))
  
