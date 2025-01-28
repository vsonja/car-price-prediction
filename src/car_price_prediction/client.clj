(ns car-price-prediction.client
  (:require [clj-http.client :as http]
            [car-price-prediction.config :refer [api-key, search-url, decode-url, history-url]]))

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
        (println "Failed to fetch price statistics:" (.getMessage e))))))

(defn fetch-vins-from-search [make model year]
  (let [url (str search-url
                 "?api_key=" api-key
                 "&make=" make
                 "&model=" model
                 "&year=" year
                 "&rows=" 50)]
    (try
      (let [response (http/get url {:as :json})
            listings (:listings (:body response))]
        (->> listings
             (map :vin)
             (filter some?)
             distinct))
      (catch Exception e
        (println "Failed to search for VINs:" (.getMessage e))
        []))))

(defn vin-decoder [vin]
  (let [url (str decode-url
                 vin
                 "/specs"
                 "?api_key=" api-key)]
    (try
      (let [response (http/get url {:as :json})]
        (:body response))
      (catch Exception e
        (println "Failed to decode VIN:" (.getMessage e))))))

(defn fetch-vin-history [vin]
  (loop [page 1
         results []]
    (let [url (str history-url
                   vin
                   "?api_key=" api-key
                   "&page=" page)
          response (try (http/get url {:as :json})
                        (catch Exception e
                          (println "Failed to fetch page" page ":" (.getMessage e))
                          nil))
          listings (:body response)]
      (cond
        (nil? listings) (do
                          (println "No more pages or error on page" page ", stopping.")
                          results)
        (< (count listings) 50) (into results listings)
        :else (recur (+ page 2) (into results listings))))))
