(comment(
  This is my first little Clojure program.  All it does is store all wikipedia pages.  
  TODO:
    - have wikimedia_url use query_params object
    - 
))

(ns wikimedia.core
  (require [clojure.java.jdbc :as sql]
           [clj-http.client :as client]
           [clojure.data.json :as json]
           [cemerick.url :refer (url url-encode)]))

(def db-conn-vars { 
  :classname "org.postgresql.Driver" 
  :subprotocol "postgresql" 
  :subname "//localhost:6432/wikimedia" 
  :password (System/getenv "POSTGRES_PASSWORD")
})

(def wikimedia_url "https://en.wikipedia.org/w/api.php?action=query&list=allpages&format=json&aplimit=500&titles=Main%20Page&apfrom=")

(comment
(def query-params{
  :query-params {
    "action"  "query"
    "list"    "allpages"
    "format"  "json"
    "aplimit" "10"
    "titles"  "Main Page"
    "apfrom"    ""
  }
}))

(defn build-url [apfrom] (str wikimedia_url apfrom))
(defn get-json-response [target_url] (client/get target_url))
(defn get-body [json-data] (json/read-str (:body json-data)))
(defn get-continue-from [body] (get (get body "continue") "apcontinue"))
(defn get-pages [body] (get (get body "query") "allpages"))

(defn insert-page-into-db [page]
  (try
    (sql/insert! db-conn-vars :pages
      [:title, :pageid]
      [(get page "title"), (Integer. (get page "pageid"))]) (catch Exception e (println e))))

(defn insert-pages-into-db [pages]
  ; run! is an unlazied map
  (run! #( insert-page-into-db %1) pages))

(defn get-wikimedia-pages [apfrom]
  ; is it good functional style to use this kind of procedural pattern?
  (let [body (get-body (get-json-response (build-url apfrom)))]
    (let [continue-from (url-encode (get-continue-from body)) pages (get-pages body)]
      (insert-pages-into-db pages)
      (dosync (println apfrom))
      (if (not (nil? continue-from))
        (recur continue-from)))))

(defn -main []
  (get-wikimedia-pages ""))
