(ns xyloobservations.authfunctions
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [xyloobservations.db.core :as db]))

(defn create-user! [login password]
  (jdbc/with-transaction [t-conn db/*db*]
    (if-not (empty? (db/get-user-for-auth* t-conn {:login login}))
      (throw (ex-info "User already exists!"
                      {:xyloobservations/error-id ::duplicate-user
                       :error "User already exists!"}))
      (db/create-user!* t-conn
                        {:login    login
                         :password (hashers/derive password)}))))

(defn authenticate-user [login password]
  (let [{hashed :password :as user} (db/get-user-for-auth* {:login login})]
    (when (hashers/check password hashed)
      (dissoc user :password))))
