(defproject car-price-prediction "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "1.1.0"]
                 [midje "1.10.10"]
                 [com.github.seancorfield/next.jdbc "1.3.955"]
                 [com.mysql/mysql-connector-j "8.3.0"]
                 [cheshire "5.11.0"]
                 [clj-http "3.13.0"]
                 [incanter "1.9.3"]]
  :main ^:skip-aot car-price-prediction.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
