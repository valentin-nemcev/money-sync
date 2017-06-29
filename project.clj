(defproject money-sync "0.1.0-SNAPSHOT"
  :url "https://github.com/valentin-nemcev/money-sync"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [clj-time "0.13.0"]
                 [clojurewerkz/money "1.10.0"]
                 [pjstadig/humane-test-output "0.8.2"]
                 [org.clojure/tools.trace "0.7.9"]]
  :plugins [[lein-cljfmt "0.5.6"]]
  :injections [(require 'pjstadig.humane-test-output)
                (pjstadig.humane-test-output/activate!)]
  :main ^:skip-aot money-sync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
