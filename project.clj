(defproject yaourt-iat "0.1.0-SNAPSHOT"
  :description "Yaourt IAT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.omcljs/om "1.0.0-alpha14"]
                 [figwheel-sidecar "0.4.0" :scope "provided"]
                 [bidi "1.20.3"]
                 [ring/ring "1.4.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [com.cognitect/transit-clj "0.8.281"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [cljs-http "0.1.30" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]
                 [http-kit "2.1.19"]
                 [clj-aws-s3 "0.3.10"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.0"]]
  :hooks [leiningen.cljsbuild]
  :omit-source true
  :aot :all
  :main yaourt-iat.server
  :cljsbuild {
    :builds [{
          :source-paths ["src"]
          :compiler {
            :output-to "resources/public/js/main.js"
            :optimizations :simple
            :pretty-print false}}]})