(ns yaourt-iat.server
  (:gen-class)
  (:require [clojure.core.async :refer [chan put! <! go go-loop]]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [ring.middleware.resource :refer [wrap-resource resource-request]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [yaourt-iat.middleware :refer [wrap-transit-response wrap-transit-params]]
            [om.next.server :as om]
            [bidi.bidi :as bidi]
            [org.httpkit.server :refer [run-server]]
            [amazonica.aws.s3 :as s3]))

(defn load-edn [filename]
  (with-open [r (io/reader filename)]
    (read (java.io.PushbackReader. r))))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def conf (atom (load-edn "resources/conf/conf.edn")))

(def write-csv (chan 1))

(defn file-save [server-conf name content]
  (with-open [uuidfile (io/writer (str (:csv-path server-conf) name))]
    (println "writing result to file : " name)
    (csv/write-csv uuidfile content)))

(defn s3-save [server-conf name content]
  (let [cred (select-keys (:s3-cred server-conf) [:access-key :secret-key :endpoint])
        bucket (:bucket (:s3-cred server-conf))
        writer (java.io.StringWriter.)
        string (do (csv/write-csv writer content)
            (.toString writer))
        input (java.io.ByteArrayInputStream. (.getBytes string))]
    (println "writing result to es3 :" name " in bucket " bucket)
    (s3/put-object cred :bucket-name bucket :key name :input-stream input)))

(go-loop []
  (let [content (<! write-csv)
        server-conf (:server @conf)
        name (str (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.))
                  "-"
                  (uuid)
                  ".csv")]
    (cond
      (:s3-cred server-conf) (s3-save server-conf name content)
      (:csv-path server-conf) (file-save server-conf name content)))
  (recur))

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

(defn api-results [req]
  (put! write-csv (into [] (:transit-params req)))
  {:status 204})

(defn index [req]
  (assoc (resource-response (str "html/index.html") {:root "public"})
         :headers {"Content-Type" "text/html"}))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (resource-request (assoc req :uri "/index.html") "public")
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
