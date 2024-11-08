(ns car-price-prediction.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn read-csv-file [file-path]
  (with-open [reader (io/reader file-path)]
    (doall (csv/read-csv reader))))

(def data (read-csv-file "resources/cars.csv"))
