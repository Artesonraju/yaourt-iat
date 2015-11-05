(ns yaourt-iat.server
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [yaourt-iat.middleware :refer [wrap-transit-response wrap-transit-params]]
            [om.next.server :as om]
            [bidi.bidi :as bidi]))

(defn load-edn [filename]
  (with-open [r (io/reader filename)]
    (read (java.io.PushbackReader. r))))

(def conf (load-edn "resources/conf/conf.edn"))

(println conf)

(def routes
  ["" {"/" :index
       "/conf"
       {:get  {[""] :conf}}
       "/results"
      {:post {[""] :results}}}])

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api-conf [req]
  (generate-response (select-keys (req :conf) [:intro :end :blocks :factors])))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn api-results [req]
  (println (:transit-params req))
  (with-open [uuidfile (io/writer (str (:csv-path (:server conf))
                                       (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.))
                                       "-"(uuid)))]
    (csv/write-csv uuidfile (into [] (:transit-params req))))
  {:status 204})

(defn index [req]
  (assoc (resource-response (str "html/index.html") {:root "public"})
         :headers {"Content-Type" "text/html"}))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index nil
      :conf (api-conf (assoc req :conf conf))
      :results (api-results req)
      nil)))

(def app
  (-> handler
      (wrap-resource "public")
      wrap-reload
      wrap-transit-response
      wrap-transit-params))
