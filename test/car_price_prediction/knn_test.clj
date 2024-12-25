(ns car-price-prediction.knn-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [car-price-prediction.knn :refer :all]))

(facts "Split a dataset into 80% train and 20% test sets."
       (let [dataset [1 2 3 4 5 6 7 8 9 10]
             [train test] (train-test-split dataset)]

         (count train) => 8
         (count test) => 2

         (+ (count train) (count test)) => (count dataset)

         (train-test-split dataset) =not=> (train-test-split dataset)))

;; Finding k data points with the smallest distances from a given input and predicting price based on the average value.
(facts "kNN regression using Gower's distance."
       (let [dataset [{:brand "Mercedes-Benz", :model "GLC 300", :year 2022, :mileage 1408, :fuel_type "Gasoline", :engine 2.0, :transmission "Automatic", :price 46798}
                      {:brand "Honda", :model "Insight EX", :year 2020, :mileage 64200, :fuel_type "Hybrid", :engine 1.5, :transmission "CVT Transmission", :price 20499}
                      {:brand "BMW", :model "530i xDrive", :year 2020, :mileage 23195, :fuel_type "Gasoline", :engine 2.0, :transmission "8-Speed Automatic", :price 38900}
                      {:brand "Volkswagen", :model "Tiguan 2.0T SE", :year 2022, :mileage 11000, :fuel_type "Gasoline", :engine 2.0, :transmission "Automatic", :price 27900}
                      {:brand "Toyota", :model "Camry Hybrid XLE", :year 2018, :mileage 62200, :fuel_type "Hybrid", :engine 2.5, :transmission "CVT Transmission", :price 23995}]
             input {:brand "Volkswagen", :model "Golf GTI", :year 2022, :mileage 6400, :fuel_type "Gasoline", :engine 2.0, :transmission "Automatic", :price 41500}
             neighbors (k-nearest-neighbors dataset input 3)]

         (map :model neighbors) => ["Tiguan 2.0T SE" "GLC 300" "530i xDrive"]
         
         (predict-price neighbors 3) => 37866.0))
