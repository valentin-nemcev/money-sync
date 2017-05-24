(ns money-sync.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(defn parse-csv-file
  [fname]
  (with-open [file (io/reader fname)]
    (csv/read-csv (slurp file)))) ; todo work in progress

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
