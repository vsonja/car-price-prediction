(ns car-price-prediction.client
  (:require [clj-http.client :as http]
            [car-price-prediction.config :refer [api-key, search-url]]))

(defn fetch-car-data [make model year]
  (let [url (str search-url
                 "?api_key=" api-key
                 "&make=" make
                 "&model=" model
                 "&year=" year)]
    (try
      (let [response (http/get url {:as :json})]
        (:body response))
      (catch Exception e
        (println "Failed to fetch data:" (.getMessage e))))))

(defn fetch-price-statistics [make model year mileage fuel_type engine transmission]
  (let [url (str search-url
                 "?api_key=" api-key
                 "&make=" make
                 "&model=" model
                 "&year=" year
                 "&miles_range=" mileage
                 "&fuel_type=" fuel_type
                 "&engine=" engine
                 "&transmission=" transmission
                 "&stats=price")]
    (try
      (let [response (http/get url {:as :json})]
        (:stats (:body response)))
      (catch Exception e
        (println "Failed to fetch data:" (.getMessage e))))))

(fetch-car-data "Volkswagen" "Golf" "2020")
(fetch-price-statistics "Volkswagen" "Golf" "2020" "20000-40000" "Unleaded" "1.4L I4" "Automatic")
