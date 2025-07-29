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

(facts "Calculating similarity using Weighted Sum."
       (let [test-data [{:brand "BMW", :model "M240 i xDrive", :year "2024", :mileage "2010", :fuel_type "Gasoline", :engine "382.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T", :price "55000"}
                        {:brand "BMW", :model "840 i xDrive", :year "2024", :mileage "1500", :fuel_type "Gasoline", :engine "335.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T", :price "90000"}
                        {:brand "Lexus", :model "RX 450h Base", :year "2022", :mileage "15833", :fuel_type "Hybrid", :engine "308.0HP 3.5L V6 Cylinder Engine Gas/Electric Hybrid", :transmission "CVT Transmission", :price "52000"}]
             target {:brand "BMW", :model "M240 i xDrive", :year "2024", :mileage "2010", :fuel_type "Gasoline", :engine "382.0HP 3.0L Straight 6 Cylinder Engine Gasoline Fuel", :transmission "A/T"}]
         (calculate-similarity target (first test-data)) => 1.0
         (calculate-similarity target (second test-data)) => (roughly 0.68 0.01)
         (calculate-similarity target (nth test-data 2)) => (roughly 0.18 0.01)))

(fact "Predict July 2025 based on partial historical data."
      (let [history [{:month "2024-01", :average-price 49282.5}
                     {:month "2024-02", :average-price 30079.14285714286}
                     {:month "2024-03", :average-price 30640.47619047619}
                     {:month "2024-04", :average-price 32641.57142857143}
                     {:month "2024-05", :average-price 29899.0}
                     {:month "2024-08", :average-price 38766.625}
                     {:month "2024-09", :average-price 68227.5}
                     {:month "2024-10", :average-price 44252.39166666667}
                     {:month "2024-11", :average-price 28436.59210526316}
                     {:month "2024-12", :average-price 27523.35294117647}
                     {:month "2025-01", :average-price 26477.35294117647}
                     {:month "2025-02", :average-price 26574.89361702128}]
            n-months 5 ;; March to July.
            now (parse-month "2025-02")
            result (linear-regression history n-months now)
            july (some #(when (= (:month %) "2025-07") %) result)]

        (Double/parseDouble (clojure.string/replace (:predicted-price july) "," ".")) => (roughly 30000 1000)))

