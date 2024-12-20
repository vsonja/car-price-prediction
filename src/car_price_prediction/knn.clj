(ns car-price-prediction.knn
  (:require [car-price-prediction.core :refer :all]))

(def numerical-columns [:year :mileage :engine])
(def categorical-columns [:brand :model :fuel_type :transmission])

(defn calculate-ranges []
  (reduce
   (fn [ranges column]
     (let [values (map column data)
           range (- (apply max values) (apply min values))]
       (assoc ranges column range)))
   {}
   numerical-columns))

(def ranges (calculate-ranges))

;; 0 - identical, 1 - different.
(defn gower's-distance [row input]
  (let [numerical-distance (fn [column] (/ (Math/abs (- (get row column) (get input column)))
                                           (get ranges column)))
        categorical-distance (fn [column] (if (= (get row column) (get input column)) 0 1))
        total-numerical (reduce + (map numerical-distance numerical-columns))
        total-categorical (reduce + (map categorical-distance categorical-columns))]
    (/ (+ total-numerical total-categorical)
       (+ (count numerical-columns) (count categorical-columns)))))

(defn train-test-split [dataset]
  (let [shuffled (shuffle dataset)
        train-size (int (* 0.8 (count dataset)))]
    [(take train-size shuffled) (drop train-size shuffled)]))

(defn k-nearest-neighbors [train input k]
  (->> train
       (map #(assoc % :distance (gower's-distance % input)))
       (sort-by :distance)
       (take k)))

(defn evaluate []
  (let [[train test] (train-test-split data)
        input (first test)
        neighbors (k-nearest-neighbors train input 5)]
    (println input)
    (println "5 Nearest Neighbors:")
    neighbors))
