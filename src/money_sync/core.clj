(ns money-sync.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(defn parse-csv-file
  [fname]
  (csv/read-csv (slurp fname :encoding "cp1251") :separator \;))

(defn -main
  [& args]
  (parse-csv-file "./resources/transactions.csv"))
