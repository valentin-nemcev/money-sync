(defproject money-sync "0.1.0-SNAPSHOT"
  :url "https://github.com/valentin-nemcev/money-sync"
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :main ^:skip-aot money-sync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
