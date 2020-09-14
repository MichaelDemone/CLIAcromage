(defproject acromage "0.1.0"
  :description "A CLI version of Acromage from the Might and Magic series"
  :url "https://github.com/MichaelDemone/CLIAcromage"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"] [org.clojure/data.csv "1.0.0"] [clojure-lanterna "0.9.7"]]
  :main ^:skip-aot acromage.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
