(ns money-sync.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [clj-time.core :as time.core]
            [clj-time.format :as time.format]
            [clojurewerkz.money.amounts :as money.amounts]
            [clojurewerkz.money.format :as money.format]))

(defn parse-csv-file
  [fname]
  (csv/read-csv (slurp fname :encoding "cp1251") :separator \;))

(defn csv-data->maps
  [csv-data]
  (map zipmap
       (repeat (first csv-data))
       (rest csv-data)))

(defn process-account-num
  [row]
  {:account-num (row "Номер счета")})

(defn process-card-num
  [row]
  {:card-num (re-find #"^[\d+]{16}+" (row "Описание операции"))})

(defn process-date
  [row]
  {:date (time.format/parse (time.format/formatter "dd.MM.yy") (row "Дата операции"))})

(defn process-money
  [row]
  (let [currency (row "Валюта")
        income   (Double/parseDouble (string/replace (row "Приход") "," "."))
        outcome  (Double/parseDouble (string/replace (row "Расход") "," "."))]
    {:amount (money.amounts/parse (str currency (- income outcome)))}))

(defn process-type
  [row]
  (let [reference (row "Референс проводки")]
    {:type (cond
             (= reference "HOLD") :hold
             (string/starts-with? reference "CRD_") :card
             (string/starts-with? reference "MO") :bank_fee
             (string/starts-with? reference "OP") :salary
             (string/starts-with? reference "A") :salary
             (string/starts-with? reference "B") :transfer
             (string/starts-with? reference "C") :payment
             (= reference "__initial") :initial
             :else :unknown)}))

(defn process-row
  [row]
  (into {} (map
             #(% row)
             [process-account-num process-type process-card-num process-date process-money])))

(defn sum-accounts
  [rows]
  (into {} (for
             [[acc acc-rows] (group-by :account-num rows)]
             [acc (reduce money.amounts/plus (map :amount acc-rows))])))

(defn print-accounts
  [accounts]
  (doseq [[acc balance] accounts]
    (println acc (money.format/format balance))))

(defn -main
  [& files]
  (->> files
       (map #(map process-row (csv-data->maps (parse-csv-file %1))))
       flatten
       (sort-by :date)
       sum-accounts
       print-accounts))

