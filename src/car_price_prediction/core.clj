(ns car-price-prediction.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn read-csv-file [file-path]
  (with-open [reader (io/reader file-path)]
    (let [rows (csv/read-csv reader)
          headers (map keyword (first rows))]
      (doall (map #(zipmap headers %) (rest rows))))))

(def data (read-csv-file "resources/cars.csv"))

(defn main-menu []
  (println "\nMenu Options:")
  (println "1. View all cars")
  (println "2. Search for specific car")
  (println "3. Exit")
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
                        (assoc acc column value))))
                  {}
                  columns)

        results (filter
                 (fn [row]
                   (every? (fn [[column value]]
                             (= (get row column) value))
                           criteria))
                 data)]

    (if (seq results)
      (do
        (println "\nResults matching search criteria:")
        (doseq [row results]
          (println row))
        (println "\nWould you like to calculate average price? (Yes/No)")
        (let [response (read-line)]
          (when (= response "Yes")
            (println "Average price:" (calculate-average-price results)))))
      (println "\nNo matching results."))))

(defn -main [& args]
  (println "Welcome to the Car Price Prediction App!")
  (loop []
    (main-menu)
    (let [option (read-line)]
      (cond
        (= option "1") (do (view-all) (recur))
        (= option "2") (do (search) (recur))
        (= option "3") (println "Goodbye!")
        :else (do (println "Invalid option! Please try again.") (recur))))))
