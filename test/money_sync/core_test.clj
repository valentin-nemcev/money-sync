(ns money-sync.core-test
  (:require [clojure.test :refer :all]
            [money-sync.core :refer :all]
            [clj-time.core :as time.core]
            [clojurewerkz.money.amounts :as money.amounts]))

(deftest parse-transactions
  (is (= (list {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "22.05.17",
                "Референс проводки" "HOLD",
                "Описание операции" "10357009 RU PEREKRESTOK SVERDLOVSK> 17.05.22 17.05.22 952.00 RUR 548673++++++3137",
                "Приход"            "0",
                "Расход"            "952",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "21.05.17",
                "Референс проводки" "MOWV 21705000584",
                "Описание операции" "Ком.за пер.с исп.рас.карты ОАО\"АЛЬФА-БАНК \",вып.к Тек.сч.Кл.,на сч.карты стор.банк.Согл.тар.Банка Немцева Юлия Сергеевна",
                "Приход"            "0",
                "Расход"            "185,25",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "21.05.17",
                "Референс проводки" "CRD_4XR24Z",
                "Описание операции" "548673++++++3137    10357010\\RUS\\SANKT PETERBU\\PEREKRESTOK S          21.05.17 18.05.17      1334.00  RUR MCC5411",
                "Приход"            "0",
                "Расход"            "1334",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "21.05.17",
                "Референс проводки" "CRD_7U9213",
                "Описание операции" "510126++++++6530      809131\\643\\MOSKVA\\CARD2CARD CLK                 21.05.17 20.05.17      9500.00  RUR MCC6012",
                "Приход"            "0",
                "Расход"            "9500",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "20.05.17",
                "Референс проводки" "C012005170005237",
                "Описание операции" "Перевод                                                               01N 45415 2005 40817810108900008075044525593 30101810200000000593",
                "Приход"            "0",
                "Расход"            "7000",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810807150018598",
                "Валюта"            "RUR",
                "Дата операции"     "19.05.17",
                "Референс проводки" "OP1ED02112339436",
                "Описание операции" "Зарплата                                                              01N     1 1905 40817810903001601210044030723 30101810100000000723",
                "Приход"            "15686,2",
                "Расход"            "0",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810108900008075",
                "Валюта"            "RUR",
                "Дата операции"     "11.06.17",
                "Референс проводки" "CRD_5W12YR",
                "Описание операции" "510126++++++4656    24064401\\RUS\\ARTGORBUNOV R\\ARTGORBUNOV            10.06.17 08.06.17        50.00  RUR MCC8299",
                "Приход"            "0",
                "Расход"            "50",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810108900008075",
                "Валюта"            "RUR",
                "Дата операции"     "10.06.17",
                "Референс проводки" "MOCV 10706000272",
                "Описание операции" "Ком.за пер.с исп.рас.карты ОАО\"АЛЬФА-БАНК \",вып.к Тек.сч.Кл.,на сч.карты стор.банк.Согл.тар.Банка Немцев Валентин Николаевич",
                "Приход"            "0",
                "Расход"            "30",
                ""                  ""}
               {"Тип счёта"         "Текущий счёт",
                "Номер счета"       "40817810108900008075",
                "Валюта"            "RUR",
                "Дата операции"     "10.06.17",
                "Референс проводки" "CRD_72C25V",
                "Описание операции" "510126++++++4656      809131\\643\\MOSKVA\\CARD2CARD CLK                 10.06.17 09.06.17       500.00  RUR MCC6012",
                "Приход"            "0",
                "Расход"            "500",
                ""                  ""})
         (csv-data->maps (parse-csv-file "./resources/transactions.csv")))))

(deftest test-process-row
  (is (= (list
           {:account-num "40817810807150018598"
            :type        :hold,
            :card-num    nil,
            :date        (time.core/date-time 2017 5 22)
            :amount      (money.amounts/parse "RUR -952.00")}
           {:account-num "40817810807150018598"
            :type        :bank_fee,
            :card-num    nil,
            :date        (time.core/date-time 2017 5 21)
            :amount      (money.amounts/parse "RUR -185.25")}
           {:account-num "40817810807150018598"
            :type        :card,
            :card-num    "548673++++++3137",
            :date        (time.core/date-time 2017 5 21)
            :amount      (money.amounts/parse "RUR -1334.00")}
           {:account-num "40817810807150018598"
            :type        :card,
            :card-num    "510126++++++6530",
            :date        (time.core/date-time 2017 5 21)
            :amount      (money.amounts/parse "RUR -9500.00")}
           {:account-num "40817810807150018598"
            :type        :payment,
            :card-num    nil,
            :date        (time.core/date-time 2017 5 20)
            :amount      (money.amounts/parse "RUR -7000.00")}
           {:account-num "40817810807150018598"
            :type        :salary,
            :card-num    nil,
            :date        (time.core/date-time 2017 5 19)
            :amount      (money.amounts/parse "RUR 15686.20")}
           {:account-num "40817810108900008075",
            :type        :card,
            :card-num    "510126++++++4656",
            :date        (time.core/date-time 2017 6 11),
            :amount      (money.amounts/parse "RUR -50.00")}
           {:account-num "40817810108900008075",
            :type        :bank_fee,
            :card-num    nil,
            :date        (time.core/date-time 2017 6 10),
            :amount      (money.amounts/parse "RUR -30.00")}
           {:account-num "40817810108900008075",
            :type        :card,
            :card-num    "510126++++++4656",
            :date        (time.core/date-time 2017 6 10),
            :amount      (money.amounts/parse "RUR -500.00")})
         (map process-row (csv-data->maps (parse-csv-file "./resources/transactions.csv"))))))

(deftest test-process-row-initial
  (is (= {:account-num "40817810108900008075",
          :type        :initial,
          :card-num    nil,
          :date        (time.core/date-time 2017 6 10),
          :amount      (money.amounts/parse "RUR 15000.00")}
         (process-row
           {"Тип счёта"         "Текущий счёт",
            "Номер счета"       "40817810108900008075",
            "Валюта"            "RUR",
            "Дата операции"     "10.06.17",
            "Референс проводки" "__initial",
            "Описание операции" "",
            "Приход"            "15000",
            "Расход"            "0"}))))

(deftest test-sum-accounts
  (is (= {"40817810807150018598" [(money.amounts/parse "RUR -3285.05") (time.core/date-time 2017 5 22)],
          "40817810108900008075" [(money.amounts/parse "RUR -580.00") (time.core/date-time 2017 6 11)]}
         (sum-accounts (map process-row (csv-data->maps (parse-csv-file "./resources/transactions.csv")))))))
