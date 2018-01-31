(ns money-sync.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [clj-time.core :as time.core]
            [clj-time.format :as time.format]
            [clojurewerkz.money.amounts :as money.amounts]
            [clojurewerkz.money.format :as money.format]
            [clojure.core.match :refer [match]]))

(. clojure.pprint/simple-dispatch addMethod
   org.joda.time.DateTime #(print (time.format/unparse
                                    (time.format/formatters :date)
                                    %)))

(. clojure.pprint/simple-dispatch addMethod
   org.joda.money.Money #(print (money.format/format %)))

(defn p
  ([str arg] (print str "=> ") (doto arg clojure.pprint/pprint))
  ([arg] (doto arg clojure.pprint/pprint)))

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
  (let [card-num (re-find #"\d{6}\+{6}\d{4}" (row "Описание операции"))]
    {:card-num   card-num
     :proc-descr (string/replace (processed :proc-descr) (or card-num "") "")}))

(defn parse-date
  [fmt str]
  (and str (time.format/parse (time.format/formatter fmt) str)))

(defn process-report-date
  [row processed]
  {:report-date (parse-date "dd.MM.yy" (row "Дата операции"))})

(defn process-date
  [row processed]
  (let
    ; proc-date-str is probably a date when transaction was processed by processing system
    [[match proc-date-str date-str]
     (re-find #"(\d\d\.\d\d\.\d\d) (\d\d\.\d\d\.\d\d)" (row "Описание операции"))

     fmt
     (if (= (processed :type) :hold) "yy.MM.dd" "dd.MM.yy")

     date
     (or (parse-date fmt date-str) (processed :report-date))]

    {:date       date
     :proc-descr (-> processed
                     :proc-descr
                     (string/replace (or proc-date-str "") "")
                     (string/replace (or date-str "") ""))}))

(defn process-account-amount
  [row processed]
  (let [currency (row "Валюта")
        income   (Double/parseDouble (string/replace (row "Приход") "," "."))
        outcome  (Double/parseDouble (string/replace (row "Расход") "," "."))]
    {:account-amount (money.amounts/parse (str currency (- income outcome)))}))

(defn process-amount
  [row processed]
  (let
    [[match amount-str currency-str]
     (re-find #"(\d+\.\d{2})  ?([A-Z]{3})" (row "Описание операции"))

     negative
     (money.amounts/negative? (processed :account-amount))

     amount
     (if (nil? amount-str)
       (processed :account-amount)
       (money.amounts/parse (str currency-str (if negative "-" "") amount-str)))]

    {:amount     amount
     :proc-descr (-> processed
                     :proc-descr
                     (string/replace (or amount-str "") "")
                     (string/replace (or currency-str "") ""))}))

(defn process-type
  [row processed]
  (let [reference (row "Референс проводки")]
    {:ref  reference
     :type (cond
             (= reference "HOLD") :hold
             (string/starts-with? reference "CRD_") :card
             (string/starts-with? reference "MO") :bank_fee
             (string/starts-with? reference "OP") :salary
             (string/starts-with? reference "A") :salary
             (string/starts-with? reference "B") :transfer
             (string/starts-with? reference "C") :payment
             (= reference "__initial") :initial
             :else :unknown)}))

(defn process-description
  [row processed]
  {:proc-descr (row "Описание операции")})

(defn process-proc-description
  [row processed]
  {:proc-descr (-> processed
                   :proc-descr
                   (string/replace #"\s+" " ")
                   string/trim)})


(defn process-row
  [row]
  (reduce
    (fn [res f] (merge res (f row res)))
    {}
    [process-description
     process-type
     process-account-num
     process-card-num
     process-report-date
     process-date
     process-account-amount
     process-amount
     process-proc-description]))

(defn accounts-stat
  [rows]
  (into [] (for
             [[acc acc-rows] (group-by :account-num rows)
              :let [holds (filter (fn [row] (= (:type row) :hold)) acc-rows)]]

             {:account
              acc

              :balance
              (reduce money.amounts/plus (map :account-amount acc-rows))

              :last-updated-date
              (reduce time.core/max-date (map :report-date acc-rows))

              :hold-total-amount
              (if (empty? holds)
                nil
                (money.amounts/abs (reduce money.amounts/plus
                                           (map :account-amount holds))))

              :first-hold-date
              (if (empty? holds)
                nil
                (reduce time.core/min-date (map :report-date holds)))})))

(defn print-accounts
  [accounts]
  (clojure.pprint/print-table
    (for [{:keys [account balance last-updated-date hold-total-amount first-hold-date]} accounts]
      {"Account"
       account

       "Total amount"
       (money.format/format balance)

       "Last transaction date"
       (time.format/unparse (time.format/formatter "dd.MM.yy") last-updated-date)

       "Hold total amount"
       (if (nil? hold-total-amount)
         "no holds"
         (money.format/format hold-total-amount))

       "First hold date"
       (if (nil? first-hold-date)
         "no holds"
         (time.format/unparse (time.format/formatter "dd.MM.yy") first-hold-date))})))

(defn print-rows
  [rows]
  (clojure.pprint/print-table
    (map
      (fn [{:keys [account-num type ref card-num date amount description proc-descr history]}]
        {"Account"    account-num
         "Type"       type
         "Card"       card-num
         "Date"       (time.format/unparse (time.format/formatter "dd.MM.yy") date)
         "Ref"        ref
         "Amount"     (and amount (money.format/format amount))
         "History"    (string/join " " (map :ref history))
         "Proc descr" proc-descr})
      rows)))

(def row-merge-key (juxt
                     :card-num
                     (comp money.amounts/currency-of :amount)
                     (comp money.amounts/minor-of :amount)
                     :ref))

(def row-hold-key #(mapv % [:card-num :amount]))

(def is-hold? #(= (% :type) :hold))


(defn rows-id?
  "Rows identical"
  [left right]
  (if (or (is-hold? left) (is-hold? right))
    (= (row-hold-key left) (row-hold-key right))
    (= (:ref left) (:ref right))))

(defn row-lt?
  "Row less than"
  [left right]
  (neg? (compare (row-merge-key left) (row-merge-key right))))

(defn flatten-hist
  [rows]
  (mapcat #(into [(dissoc % :history)] (% :history)) rows))

(defn add-to-hist
  [& history-rows]
  (let [[row & history] (->> history-rows
                             flatten-hist
                             set
                             (sort-by :report-date)
                             reverse)]
    (assoc row :history (vec history))))

(defn merge-row-lists
  [prev next]
  (loop [left  (vec (sort-by row-merge-key prev))
         right (vec (sort-by row-merge-key next))
         res   []]
    (match
      [left     right   ]
      [[l & lr] [r & rr]] (cond
                            (rows-id? l r) (recur lr   rr    (conj res (add-to-hist r l)))
                            (row-lt?  l r) (recur lr   right (conj res (add-to-hist l)))
                            :else          (recur left rr    (conj res (add-to-hist r))))
      [[]       [r & rr]] (recur [] rr (conj res (add-to-hist r)))
      [[l & lr] []      ] (recur lr [] (conj res (add-to-hist l)))
      [[]       []      ] res)))

(defn merge-rows
  [res input]
  (let [inputMap (group-by #(mapv % [:account-num :date]) input)]
    (merge-with merge-row-lists res inputMap)))


(defn -main
  [& files]
  (->> files
       (map #(map process-row (csv-data->maps (parse-csv-file %1))))
       (reduce merge-rows (sorted-map))
       vals
       (map #(sort-by :ref %))
       flatten
       print-rows))
       ; (sort-by :report-date)
       ; accounts-stat
       ; print-accounts))

