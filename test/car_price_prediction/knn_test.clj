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
