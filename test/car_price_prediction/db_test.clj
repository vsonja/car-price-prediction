(ns car-price-prediction.db-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [car-price-prediction.db :refer :all]))

;; As a user, I want to save my search criteria and results so that I can review them later without repeating search process.
(facts "Saving and Viewing searches."
       (let [user-id "80e6218f-240b-4555-9337-b0995ace5e32"
             criteria {:brand "Lexus", :year "2022", :fuel_type "Hybrid"}
             results [{:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}
                      {:brand "Lexus", :model "RX 450h F Sport Handling", :year "2022", :mileage "1600", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "A/T", :price "59000"}]]

         (fact "Saving search criteria with results."
               (save-search user-id criteria results) => [{:next.jdbc/update-count 1}])

         (fact "Retrieving searches saved by the current user."
               (get-saved-searches user-id) => (contains [{:criteria criteria
                                                           :results results}]))))