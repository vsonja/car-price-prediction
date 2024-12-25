(ns car-price-prediction.knn
  (:require [car-price-prediction.core :as core]
            [incanter.core :as incanter]
            [incanter.charts :as charts]))

(def numerical-columns [:year :mileage :engine])
(def categorical-columns [:brand :model :fuel_type :transmission])

(defn calculate-ranges []
  (reduce
   (fn [ranges column]
     (let [values (map column core/data)
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

;; (defn predict-price [train input k]
;;   (let [neighbors (k-nearest-neighbors train input k)
;;         prices (map :price neighbors)]
;;     (double (/ (reduce + prices) (count prices)))))

(defn predict-price [neighbors k]
  (let [prices (map :price (take k neighbors))]
    (double (/ (reduce + prices) (count prices)))))

(defn mean-absolute-error [predicted actual]
  (/ (reduce + (map #(Math/abs (- %1 %2)) predicted actual))
     (count predicted)))

(defn elbow-method [train test]
  (let [neighbors (map #(k-nearest-neighbors train % 10) test)
        results (for [k (range 2 11)]
                  (let [predicted (map #(predict-price % k) neighbors)
                        actual (map :price test)]
                    {:k k :mae (mean-absolute-error predicted actual)}))
        k (map :k results)
        mae (map :mae results)
        graph (charts/xy-plot k mae
                              :title "Elbow Method"
                              :x-label "k"
                              :y-label "MAE")]

    (incanter/view graph)

    (:k (apply min-key :mae results))))

(defn evaluate []
  (let [[train test] (train-test-split core/data)
        input (first test)
        k (elbow-method train test)
        neighbors (k-nearest-neighbors train input k)]
    (println "k =" k)
    (println input)
    (println "Predicted price:" (predict-price neighbors k))))
