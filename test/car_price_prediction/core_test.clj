(ns car-price-prediction.core-test
  (:require [midje.sweet :refer :all] 
            [clojure.test :refer :all]
            [car-price-prediction.core :refer :all]))

;; As a user, I want to view all available data about cars so that I can explore the options.
(facts "Displaying data about all cars."
      (view-all) :truthy)

;; As a user, I want to search for cars based on specific criteria (e.g. make, model, year) so that I can quickly find what I'm looking for.
(facts "Searching for specific car."
       (apply-filter {}) => data

       (apply-filter {:model "840 i xDrive"})
       => [{:brand "BMW", :model "840 i xDrive", :year "2024", :mileage "1500", :fuel_type "Gasoline", :engine "335.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T", :price "90000"}]
       
       (apply-filter {:brand "Lexus", :year "2022", :fuel_type "Hybrid"})
       => [{:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}
           {:brand "Lexus", :model "RX 450h F Sport Handling", :year "2022", :mileage "1600", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "A/T", :price "59000"}])

;; As a user, I want to calculate average price of search results so that I can perceive the price for specific category.
(facts "Calculating average price of search results."
       (let [results [{:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}
                      {:brand "Lexus", :model "RX 450h F Sport Handling", :year "2022", :mileage "1600", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "A/T", :price "59000"}]]
         (calculate-average-price results) => "55500,00"))

;; As a user, I want to save my search criteria and results so that I can review them later without repeating search process.
(facts "Saving search criteria with results."
      (let [criteria {:brand "Lexus", :year "2022", :fuel_type "Hybrid"}
            results [{:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}
                     {:brand "Lexus", :model "RX 450h F Sport Handling", :year "2022", :mileage "1600", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "A/T", :price "59000"}]]
        (save-search criteria results)
        @saved-searches => (contains {:parameters criteria :results results})))

(facts "Calculating similarity using Weighted Sum."
       (let [test-data [{:brand "BMW", :model "M240 i xDrive", :year "2024", :mileage "2010", :fuel_type "Gasoline", :engine "382.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T", :price "55000"}
                        {:brand "BMW", :model "840 i xDrive", :year "2024", :mileage "1500", :fuel_type "Gasoline", :engine "335.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T", :price "90000"}
                        {:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}]
             target {:brand "BMW", :model "M240 i xDrive", :year "2024", :mileage "2010", :fuel_type "Gasoline", :engine "382.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T"}]
         (calculate-similarity target (first test-data)) => 1.0
         (calculate-similarity target (second test-data)) => (roughly 0.68 0.01)
         (calculate-similarity target (nth test-data 2)) => (roughly 0.18 0.01)))
