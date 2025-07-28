(ns car-price-prediction.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [car-price-prediction.db :as db]
            [car-price-prediction.client :as client]
            [incanter.core :as incanter]
            [incanter.stats :as stats]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clojure.pprint :as pprint]))

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
  (println "1. View all available cars")
  (println "2. Search for a specific car")
  (println "3. Open saved searches")
  (println "4. Estimate current price")
  (println "5. Predict prices for the upcoming months")
  (println "6. Exit")
  (println "\nSelect an option: ")
  (flush))

(defn view-all []
  (println "\nDisplaying all cars...")
  (pprint/print-table [:brand :model :year :price :mileage] data))

(defn calculate-average-price [results]
  (let [prices (map :price results)]
    (when (seq prices)
      (format "%.2f" (/ (double (reduce + prices)) (count prices))))))

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
        (pprint/print-table (keys (first results)) results)
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
      (do
        (println "")
        (doseq [{:keys [criteria results]} saved-searches]
          (clojure.pprint/pprint criteria)
          (clojure.pprint/print-table
           [:brand :model :year :mileage :fuel_type :engine :transmission :price]
           results)
          (println))))))

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

(defn estimate-price []
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

    (println "\nEstimated price:" (format "%.2f" (if (> total-similarity 0)
                                                   (/ weighted-sum total-similarity)
                                                   0)))))

(defn date->month [date]
  (subs date 0 7)) ;; "yyyy-MM"

(defn summarize-monthly-prices [history]
  (->> history
       (filter (fn [entry]
                 (and entry
                      (:last_seen_at_date entry)
                      (:price entry))))
       (map #(assoc % :month (date->month (:last_seen_at_date %))))
       (group-by :month)
       (map (fn [[month entries]]
              {:month month
               :average-price (double (/ (reduce + (map :price entries))
                                         (count entries)))}))
       (sort-by :month)))

;; (defn vin-monthly-price-series [vin]
;;   (let [history (client/fetch-vin-history vin)
;;         series (summarize-monthly-prices history)]
;;     series))

(defn fetch-multiple-histories [vins]
  (println "\nFetching historical data...")
  (loop [[vin & rest] vins
         acc []]
    (if vin
      (let [history (client/fetch-vin-history vin)]
        (Thread/sleep 1500)
        (recur rest (into acc history)))
      acc)))

(defn generate-monthly-price-series [make model year]
  (let [vins (client/fetch-vins-from-search make model year)
        histories (fetch-multiple-histories vins)]
    (summarize-monthly-prices histories)))

(defn prompt-for-values [columns]
  (println "")
  (reduce
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
   columns))

(defn months-difference [start end]
  (time/in-months (time/interval start end)))

(defn parse-month [month]
  (format/parse (format/formatter "yyyy-MM") month))

(defn predict-price []
  (let [{:keys [make model year n-months]} (prompt-for-values [:make :model :year :n-months])
        year (Integer/parseInt year)
        n-months (Integer/parseInt n-months)
        data (generate-monthly-price-series make model year)
        start-month (parse-month (:month (first data)))
        now (time/now)

        ;; [months-from-start, price]
        x (incanter/matrix (mapv #(vector (months-difference start-month (parse-month (:month %))))
                                 data))
        y (incanter/matrix (mapv :average-price data))

        regression-model (stats/linear-model y x)
        coefs (:coefs regression-model)
        intercept (first coefs)
        slope (second coefs)

        ;; Predictions for N months from now.
        months-from-start (months-difference start-month now)
        predictions (for [i (range 1 (inc n-months))]
                      (let [future-x (+ months-from-start i)
                            predicted-price (+ intercept (* slope future-x))] ;; y = ax + b
                        {:month i
                         :predicted-price predicted-price}))

        result (map (fn [{:keys [month predicted-price]}]
                      (let [future-date (time/plus now (time/months month))
                            fmt (format/formatter "yyyy-MM")]
                        {:month (format/unparse fmt future-date)
                         :predicted-price (format "%.2f" predicted-price)}))
                    predictions)]

    ;; (println "\nHistorical data:")
    ;; (doseq [row data]
    ;;   (println row))

    (println (str "\nPredicted prices over the next " n-months " months for " make " " model " (" year "):"))
    (doseq [row result]
      (println row))))

(defn -main [& args]
  (println "Welcome to the Car Price Prediction App!")
  (loop []
    (main-menu)
    (let [option (read-line)]
      (cond
        (= option "1") (do (view-all) (recur))
        (= option "2") (do (search) (recur))
        (= option "3") (do (view-saved-searches) (recur))
        (= option "4") (do (estimate-price) (recur))
        (= option "5") (do (predict-price) (recur))
        (= option "6") (println "Goodbye!")
        :else (do (println "Invalid option! Please try again.") (recur))))))

(-main)
