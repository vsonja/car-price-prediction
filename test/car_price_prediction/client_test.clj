(ns car-price-prediction.client-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [car-price-prediction.client :refer :all]))

(facts "Search active dealer inventory using Search API."
       (fetch-car-data "Volkswagen" "Golf" "2020") =not=> nil

       (fact "Invalid value."
             (fetch-car-data "Volkswagen" "Golf" "/") => nil
             (with-out-str (fetch-car-data "Volkswagen" "Golf" "/")) => (contains "Failed to fetch data:")))

(facts "Fetch price statistics using Search API."
       (:price (fetch-price-statistics "Volkswagen" "Golf" "2020" "20000-40000" "Unleaded" "1.4L I4" "Automatic")) =not=> (contains {:count 0})

       (fact "Invalid value."
             (:price (fetch-price-statistics "Volkswagen" "Golf" "2020" "20000-40000" "Unleaded" "1.4L I4" "/")) => (contains {:count 0})))

(facts "Fetch VINs from Search API."
       (fact "Returns distinct VINs from response listings."
             (let [response {:body {:listings [{:vin "VIN1"} {:vin "VIN2"} {:vin nil} {:vin "VIN2"} {:vin "VIN3"}]}}]
               (with-redefs [http/get (fn [& _] response)]
                 (fetch-vins-from-search "Toyota" "Corolla" 2022) => ["VIN1" "VIN2" "VIN3"])))

       (fact "Empty listings."
             (with-redefs [http/get (fn [& _] {:body {:listings []}})]
               (fetch-vins-from-search "Toyota" "Corolla" 2022) => []))

       (fact "Returns empty list on Exception."
             (with-redefs [http/get (fn [& _] (throw (Exception.)))]
               (fetch-vins-from-search "Toyota" "Corolla" 2022) => []
               (with-out-str (fetch-vins-from-search "Toyota" "Corolla" 2022)) => (contains "Failed to search for VINs:"))))

(facts "Decode VIN and get the specifications for it using VIN Decoder API."
       (vin-decoder "1FAHP3F28CL148530") =not=> nil

       (fact "Invalid value."
             (vin-decoder "") => nil
             (with-out-str (vin-decoder "")) => (contains "Failed to decode VIN:")))

(facts "Fetch VIN history using VIN History API."
       (fetch-vin-history "1FTEW1EF1FFA67753") =not=> nil

       (fact "Invalid value."
             (fetch-vin-history "") => nil
             (with-out-str (fetch-vin-history "")) => (contains "Failed to fetch VIN history:")))
