(ns money-sync.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(defn parse-csv-file
  [fname]
  (csv/read-csv (slurp fname :encoding "cp1251") :separator \;))

(defn csv-data->maps
  [csv-data]
  (map zipmap
       (repeat (first csv-data))
       (rest csv-data)))

(defn retrieve-hold
  [row]
  (if (= (row "Референс проводки") "HOLD") true false))

(defn process-row
  [row]
  {:hold (retrieve-hold row)})

(defn -main
  [& args]
  ; todo not complete
  (csv-data->maps (parse-csv-file "./resources/transactions.csv")))
