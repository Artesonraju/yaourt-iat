(defproject iatrf-cljs "0.1.0-SNAPSHOT"
  :description "Psychology experiments"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.omcljs/om "1.0.0-alpha14"]
                 [testdouble/clojurescript.csv "0.2.0"]
                 [figwheel-sidecar "0.4.0" :scope "provided"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {
    :builds [{
       :source-paths ["src"]
       :compiler {:main 'iatrf-cljs.core
                  :asset-path "js"
                  :optimizations :whitespace
                  :output-to "resources/prod/js/main.js"
                  :output-dir "resources/prod/js"
                  :pretty-print true
                  :verbose true}}]})