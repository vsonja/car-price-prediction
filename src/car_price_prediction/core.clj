(ns car-price-prediction.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [car-price-prediction.db :as db]))

(defn parse-numeric-column [value column]
  (cond
    (#{:year :mileage :price} column) (Integer. value)
    (= :engine column) (Double. value)
    :else value))

(defn read-csv-file [file]
  (with-open [reader (io/reader file)]
    (let [rows (csv/read-csv reader)
          headers (mapv keyword (first rows))]
      (mapv (fn [row]
              (zipmap headers
                      (mapv (fn [[column value]] (parse-numeric-column value column))
                            (map vector headers row))))
            (rest rows)))))

(defn normalize [column]
  (let [minimum (apply min column)
        maximum (apply max column)]
    (map #(-> % (- minimum) (/ (double (- maximum minimum)))) column)))

(defn normalize-columns [dataset columns]
  (mapv (fn [column] (normalize (map #(get % column) dataset)))
        columns))

;; (def data
;;   (let [dataset (read-csv-file "resources/cars.csv")
;;         columns [:year :mileage :engine]
;;         normalized-columns (normalize-columns dataset columns)]
;;     (mapv (fn [row [normalized-year normalized-mileage normalized-engine]]
;;             (assoc row
;;                    :normalized-year normalized-year
;;                    :normalized-mileage normalized-mileage
;;                    :normalized-engine normalized-engine))
;;           dataset
;;           (apply mapv vector normalized-columns))))

(def data (read-csv-file "resources/cars.csv"))

(defn get-user-id []
  (let [user-id-file "user-id.txt"]
    (if (.exists (io/file user-id-file))
      (slurp user-id-file)
      (let [new-user-id (str (random-uuid))]
        (spit user-id-file new-user-id)
        new-user-id))))

(defn main-menu []
  (println "\nMenu Options:")
  (println "1. View all cars")
  (println "2. Search for specific car")
  (println "3. View saved searches")
  (println "4. Predict car price")
  (println "5. Exit")
  (println "Select an option: ")
  (flush))

(defn view-all []
  (println "\nDisplaying all cars...")
  (doseq [row data]
    (println row)))

(defn calculate-average-price [results]
  (let [prices (map #(Double/parseDouble (:price %)) results)]
    (when (seq prices)
      (format "%.2f" (/ (reduce + prices) (count prices))))))

(defn apply-filter [criteria]
  (filter
   (fn [row]
     (every? (fn [[column value]]
               (= (get row column) value))
             criteria))
   data))

(defn search []
  (println "")
  (let [columns (remove #{:price} (keys (first data)))
        criteria (reduce
                  (fn [acc column]
                    (println (str "Enter value for '" (name column) "' (or press Enter to skip): "))
                    (flush)
                    (let [value (read-line)]
                      (if (empty? value)
                        acc
                        (assoc acc column (parse-numeric-column value column)))))
                  {}
                  columns)
        results (apply-filter criteria)]

    (if (seq results)
      (do
        (println "\nResults matching search criteria:")
        (doseq [row results]
          (println row))
        (println "\nWould you like to calculate average price? (Yes/No)")
        (let [response (read-line)]
          (when (= response "Yes")
            (println "Average price:" (calculate-average-price results))))
        (println "\nDo you want to save this search? (Yes/No)")
        (when (= (read-line) "Yes")
          (let [user-id (get-user-id)]
            (db/save-search user-id criteria results)
            (println "Your search has been saved!"))))
      (println "\nNo matching results."))))

(defn view-saved-searches []
  (let [user-id (get-user-id)
        saved-searches (db/get-saved-searches user-id)]
    (if (empty? saved-searches)
      (println "\nNo saved searches.")
      (println saved-searches))))

(def weights {:brand 0.2 :model 0.18 :year 0.22 :mileage 0.08 :fuel_type 0.12 :engine 0.14 :transmission 0.06})

(defn calculate-similarity [target current]  
  (let [brand-similarity (* (:brand weights)
                            (if (= (:brand target) (:brand current)) 1 0))
        model-similarity (* (:model weights)
                            (if (= (:model target) (:model current)) 1 0))
        year-similarity (* (:year weights) 
                           (- 1 (/ (Math/abs (- (Integer/parseInt (:year target)) (Integer/parseInt (:year current)))) 
                                   4)))
        mileage-similarity (* (:mileage weights) 
                              (- 1 (/ (Math/abs (- (Integer/parseInt (:mileage target)) (Integer/parseInt (:mileage current)))) 
                                      (- (apply max (map #(Integer/parseInt (:mileage %)) data))
                                         (apply min (map #(Integer/parseInt (:mileage %)) data))))))
        fuel-similarity (* (:fuel_type weights)
                           (if (= (:fuel_type target) (:fuel_type current)) 1 0))
        engine-similarity (* (:engine weights)
                             (if (= (:engine target) (:engine current)) 1 0))
        transmission-similarity (* (:transmission weights)
                                   (if (= (:transmission target) (:transmission current)) 1 0))]
    
    (+ brand-similarity model-similarity year-similarity mileage-similarity fuel-similarity engine-similarity transmission-similarity)))

(defn predict-price []
  (println "")
  (let [columns (remove #{:price} (keys (first data)))
        target (reduce
                (fn [acc column]
                  (loop []
                    (println (str "Enter value for '" (name column) "': "))
                    (flush)
                    (let [value (read-line)]
                      (if (empty? value)
                        (do
                          (println "This field is mandatory!")
                          (recur))
                        (assoc acc column value)))))
                {}
                columns)
        similarities (map (fn [current]
                            (let [sim (calculate-similarity target current)]
                              {:price (Double/parseDouble (:price current))
                               :similarity sim}))
                          data)
        weighted-sum (reduce + (map #(* (:price %) (:similarity %)) similarities))
        total-similarity (reduce + (map :similarity similarities))]

    (println "\nPredicted price:" (format "%.2f" (if (> total-similarity 0)
                                                   (/ weighted-sum total-similarity)
                                                   0)))))

(defn -main [& args]
  (println "Welcome to the Car Price Prediction App!")
  (loop []
    (main-menu)
    (let [option (read-line)]
      (cond
        (= option "1") (do (view-all) (recur))
        (= option "2") (do (search) (recur))
        (= option "3") (do (view-saved-searches) (recur))
        (= option "4") (do (predict-price) (recur))
        (= option "5") (println "Goodbye!")
        :else (do (println "Invalid option! Please try again.") (recur))))))
