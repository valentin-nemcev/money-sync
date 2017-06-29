(ns money-sync.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [clj-time.core :as time.core]
            [clj-time.format :as time.format]
            [clojurewerkz.money.amounts :as money.amounts]
            [clojurewerkz.money.format :as money.format]
            [clojure.tools.trace :as trace]))

(defn parse-csv-file
  [fname]
  (csv/read-csv (slurp fname :encoding "cp1251") :separator \;))

(defn csv-data->maps
  [csv-data]
  (map zipmap
       (repeat (first csv-data))
       (rest csv-data)))

(defn process-account-num
  [row processed]
  {:account-num (row "Номер счета")})

(defn process-card-num
  [row processed]
  {:card-num (re-find #"\d{6}\+{6}\d{4}" (row "Описание операции"))})

(defn parse-date
  [fmt str]
  (and str (time.format/parse (time.format/formatter fmt) str)))

(defn process-final-date
  [row processed]
  {:final-date (parse-date "dd.MM.yy" (row "Дата операции"))})

(defn process-date
  [row processed]
  (let
    [[match proc-date-str date-str]
     (re-find #"(\d\d\.\d\d\.\d\d) (\d\d\.\d\d\.\d\d)" (row "Описание операции"))
     fmt (if (= (processed :type) :hold) "yy.MM.dd" "dd.MM.yy")
     [proc-date date]
     (map #(or (parse-date fmt %) (processed :final-date)) [proc-date-str date-str])]
    {:date date :proc-date proc-date}))

(defn process-money
  [row processed]
  (let [currency (row "Валюта")
        income   (Double/parseDouble (string/replace (row "Приход") "," "."))
        outcome  (Double/parseDouble (string/replace (row "Расход") "," "."))]
    {:amount (money.amounts/parse (str currency (- income outcome)))}))

(defn process-type
  [row processed]
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
  (reduce
    (fn [res f] (merge res (f row res)))
    {}
    [process-type
     process-account-num
     process-card-num
     process-final-date
     process-date
     process-money]))

(defn accounts-stat
  [rows]
  (into {} (for
             [[acc acc-rows] (group-by :account-num rows)
              :let [holds (filter (fn [row] (= (:type row) :hold)) acc-rows)]]
             [acc [(reduce money.amounts/plus (map :amount acc-rows))
                   (reduce time.core/max-date (map :final-date acc-rows))
                   (if (empty? holds) nil (money.amounts/abs (reduce money.amounts/plus (map :amount holds))))
                   (if (empty? holds) nil (reduce time.core/min-date (map :final-date holds)))]])))

(defn print-accounts
  [accounts]
  (clojure.pprint/print-table
    (for [[acc [balance last-updated-date hold-amount first-hold-date]] accounts]
      {"Account"               acc
       "Total amount"          (money.format/format balance)
       "Last transaction date" (time.format/unparse (time.format/formatter "dd.MM.yy") last-updated-date)
       "Hold total amount"     (if (nil? hold-amount) "no holds"(money.format/format hold-amount))
       "First hold date"       (if (nil? first-hold-date) "no holds" (time.format/unparse (time.format/formatter "dd.MM.yy") first-hold-date))})))

(defn -main
  [& files]
  (->> files
       (map #(map process-row (csv-data->maps (parse-csv-file %1))))
       flatten
       (sort-by :final-date)
       accounts-stat
       print-accounts))

