(ns money-sync.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clj-time.core :as time.core]
            [clj-time.format :as time.format]))

(defn parse-csv-file
  [fname]
  (csv/read-csv (slurp fname :encoding "cp1251") :separator \;))

(defn csv-data->maps
  [csv-data]
  (map zipmap
       (repeat (first csv-data))
       (rest csv-data)))

(defn process-hold
  [row]
  {:hold (if (= (row "Референс проводки") "HOLD") true false)})

(defn process-card-num
  [row]
  {:card-num (re-find #"^[\d+]{16}+" (row "Описание операции"))})

(defn process-date
  [row]
  {:date (time.format/parse (time.format/formatter "dd.MM.yy") (row "Дата операции"))})

(defn -main
  [& args]
  ; todo not complete
  (csv-data->maps (parse-csv-file "./resources/transactions.csv")))
