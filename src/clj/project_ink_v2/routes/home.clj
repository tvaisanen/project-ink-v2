(ns project-ink-v2.routes.home
  (:require
   [clojure.java.io :as io]
   [project-ink-v2.layout :as layout]
   [project-ink-v2.db.core :as db]
   [project-ink-v2.middleware :as middleware]
   [clojure.pprint :refer [pprint]]
   [ring.util.response]
   [ring.util.http-response :as response]
   [reitit.coercion.malli]
   [reitit.ring.coercion :as rrc]
   [clojure.tools.logging :as log]
   [java-time :as time]
   [malli.core :as m]
   [malli.transform :as mt]
   [project-ink-v2.data :as data]
   [project-ink-v2.images :as img]
   [clojure.string :as string]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn get-tattoo-by-uuid-handler [req]
  (let [db-conn     (-> req :system :db-conn)
        tattoo-uuid (-> req :path-params :tattoo-uuid java.util.UUID/fromString)
        result      (db/get-tattoo-by-uuid db-conn tattoo-uuid)]
    (response/ok result)))

(defn post-tattoo-handler [req]
  (let [db-conn (-> req :system :db-conn)
        date    (-> req :params :tattoo/date time/java-date)
        result  (db/insert-tattoo db-conn (merge (:params req) {:tattoo/date date}))]
    (response/ok result)))

(defn patch-tattoo-handler [req]
  (let [db-conn     (-> req :system :db-conn)
        tattoo-uuid (-> req :path-params :tattoo-uuid java.util.UUID/fromString)
        values      (m/decode data/Tattoo (:params req) (mt/string-transformer))
        result      (db/update-tattoo db-conn (merge values {:tattoo/uuid tattoo-uuid}))]
    (response/ok result)))

(defn get-tattoos-handler [req]
  (let [db-conn (-> req :system :db-conn)
        result (db/get-tattoos db-conn)]
    (response/ok result)))

(defn wrap-system [handler]
  (fn [req]
    (try
      (handler (assoc req :system {:db-conn db/conn
                                   :uploads-path "./uploads"}))
      (catch Throwable t
        (log/error t (.getMessage t))))))

(defn file-ext-from-mimetype [mimetype]
  (-> mimetype
      (string/split #"/")
      last))

(defn upload-file
  "
  Save uploaded file.
  Chain with request handler.
  Does not return a response.
  "
  [req]
  (try
    (if (nil? (-> req :params :image :tempfile))
      (assoc-in req [:params :image] "no image")
      (let [uploads   (-> req :system :uploads-path)
            file      (-> req :params :image :tempfile)
            mimetype  (-> req :params :image :content-type)
            ext       (-> mimetype  file-ext-from-mimetype)
            filename  (str uploads "/" (java.util.UUID/randomUUID) "." ext)
            f         (io/file filename)]
        (io/copy file f)
        (.createNewFile f)
        (img/>img-to-resize! (merge {:filename filename} file))
        (assoc-in req [:params :image] filename)))
    (catch Throwable t
      (.getMessage t)
      req)))

(defn debug-req [req]
  (response/ok {:uploaded (:params req)}))

(defn home-routes []
  [""
   {:middleware [;; omit for no, still developing without browser
                 ;; middleware/wrap-csrf
                 wrap-system
                 middleware/wrap-formats
                 rrc/coerce-exceptions-middleware
                 rrc/coerce-request-middleware
                 rrc/coerce-response-middleware]}
   ["/" {:get home-page}]
   ["/upload" {:coercion reitit.coercion.malli/coercion
               :post     {:multipart-params
                          {:body-params
                           [:map
                            [:image
                             [:map
                              [:content-type string?]
                              [:filename string?]
                              [:size number?]
                              [:tempfile string?]]]]}
                          :handler (comp debug-req upload-file)}}]
   ["/api/v1" {:coercion reitit.coercion.malli/coercion}

    ["/tattoos"
     ["" {:post {:coercion   reitit.coercion.malli/coercion
                 :handler    (comp
                              post-tattoo-handler
                              upload-file)
                 :parameters {:body data/Tattoo}}
          :get  get-tattoos-handler}]

     ["/:tattoo-uuid" {:coercion   reitit.coercion.malli/coercion
                       :parameters {:path [:map [:tattoo-uuid uuid?]]}
                       :responses  {200 {:body [:map
                                                [:db/id number?]
                                                [:tattoo/uuid uuid?]
                                                [:tattoo/title string?]]}}
                       :get        get-tattoo-by-uuid-handler
                       :patch      {:parameters {:body-params data/Tattoo}
                                    :handler    patch-tattoo-handler}}]]]])

(comment
  ["/debug" {:coercion reitit.coercion.malli/coercion
             :post     {:parameters {:body-params [:map [:tattoo/style [:and [:enum :foo] keyword?]]]}
                        :handler    (fn [rq]
                                      (pprint (m/decode [:map [:tattoo/style [:and [:enum :foo] keyword?]]]
                                                        (:body-params rq)
                                                        (mt/string-transformer)))
                                      (pprint (:params rq))
                                      (pprint rq)
                                      (response/ok (:body-params rq)))}
             :patch    {:handler (fn [rq] (response/ok (:body-params rq)))}}]
  ["/docs" {:get (fn [_]
                   (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                       (response/header "Content-Type" "text/plain; charset=utf-8")))}])
