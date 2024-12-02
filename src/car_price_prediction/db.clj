(ns car-price-prediction.db
  (:require [next.jdbc :as jdbc]
            [cheshire.core :as json]))

(def database {:dbtype "mysql"
               :dbname "car_price_prediction"
               :host "localhost"
               :port 3306
               :user "root"
               :password ""})

(defn save-search [user-id criteria results]
  (jdbc/execute! database ["INSERT INTO searches (user_id, criteria, results) VALUES (?, ?, ?)"
                           user-id (json/generate-string criteria) (json/generate-string results)]))

(defn get-saved-searches [user-id]
  (let [result (jdbc/execute! database ["SELECT criteria, results FROM searches WHERE user_id = ? ORDER BY saved_at DESC"
                                        user-id])]
    (map #(into {}
                (for [[k v] %]
                  [(keyword (last (clojure.string/split (name k) #"/")))
                   (json/parse-string v true)]))
         result)))
