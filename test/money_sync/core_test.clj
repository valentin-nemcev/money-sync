(ns money-sync.core-test
  (:require [clojure.test :refer :all]
            [money-sync.core :refer :all]))

(deftest parse-transactions
  (is (= (parse-file "transactions.csv")
         [{:currency "RUR" :date "22.05.17" :reference :description :income :outcome},
          {:currency "RUR" :date "21.05.17" :reference :description :income :outcome},
          {:currency "RUR" :date "21.05.17" :reference :description :income :outcome},
          {:currency "RUR" :date "21.05.17" :reference :description :income :outcome},
          {:currency "RUR" :date "20.05.17" :reference :description :income :outcome},
          {:currency "RUR" :date :reference :description :income :outcome}])))
