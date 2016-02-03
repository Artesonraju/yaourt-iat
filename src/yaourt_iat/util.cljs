(ns yaourt-iat.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put!]]
            [om.next :as om]
            [cognitect.transit :as t]
            [yaourt-iat.engine :refer [init-data]])
  (:import [goog.net XhrIo]))

(defn transit-conf []
  (fn [edn cb]
    (.send XhrIo "/conf"
           (fn [_]
             (this-as this
               (let [data (t/read (t/reader :json) (.getResponseText this))]
                 (cb (init-data data)))))
           "GET" (t/write (t/writer :json) edn)
           #js {"Content-Type" "application/transit+json"})))

(defn transit-results [res]
  (.send XhrIo "/results"
         (fn [_] (println "result success"))
         "POST" (t/write (t/writer :json) res)
         #js {"Content-Type" "application/transit+json"}))