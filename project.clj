(defproject dsl-stats "1.0.0"
  :description "Simple MWEB ADSL usage reporter"
  :url "https://github.com/kennethkalmer/dsl-stats"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.6.1"]
                 [clj-http "2.2.0"]
                 [enlive "1.1.6"]
                 [clj-time "0.11.0"]
                 [cprop "0.1.8"]
                 [jarohen/chime "0.1.9"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main dsl-stats.core)
