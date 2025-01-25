(ns car-price-prediction.client-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
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
