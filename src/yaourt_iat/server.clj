(ns yaourt-iat.server
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [yaourt-iat.middleware :refer [wrap-transit-response wrap-transit-params]]
            [om.next.server :as om]
            [bidi.bidi :as bidi]
            [org.httpkit.server :refer [run-server]]))

(defn load-edn [filename]
  (with-open [r (io/reader filename)]
    (read (java.io.PushbackReader. r))))

(def conf (atom (load-edn "resources/conf/conf.edn")))

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
      :conf (api-conf (assoc req :conf @conf))
      :results (api-results req)
      nil)))

(def app
    (-> handler
        (wrap-resource "public")
        wrap-reload
        wrap-transit-response
        wrap-transit-params))

(defn -main [& [port config-file]]
  (let [port (Integer. (or port 10555))
        config-file (or config-file (io/resource "conf/conf.edn"))]
    (reset! conf (load-edn config-file))
    (println @conf)
    (println (format "Starting web server on port %d." port))
    (run-server app {:port port :join? false})))