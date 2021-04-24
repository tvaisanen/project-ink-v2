(ns project-ink-v2.db.core
  (:require
   [cheshire.core :refer [generate-string parse-string]]
   [datahike.api :as d]
   ;; [datahike-postgres.core]
   [datahike.migrate :refer [export-db]]
   [clojure.tools.logging :as log]
   [project-ink-v2.config :refer [env]]
   [mount.core :refer [defstate] :as mount]
   [java-time :as time]))

;; => #atom[#datahike/DB {:max-tx 536870913 :max-eid 39} 0x127f39ed]

(def schema
  ;; enums
   ;; todo generate enums from data
  [#:db{:ident :tattoo.size/extra-small}
   #:db{:ident :tattoo.size/small}
   #:db{:ident :tattoo.size/medium}
   #:db{:ident :tattoo.size/large}
   #:db{:ident :tattoo.size/extra-large}

   #:db{:ident :tattoo.location/ankle}
   #:db{:ident :tattoo.location/ribs}
   #:db{:ident :tattoo.location/shoulder}
   #:db{:ident :tattoo.location/forearm}
   #:db{:ident :tattoo.location/innerarm}
   #:db{:ident :tattoo.location/calf}
   #:db{:ident :tattoo.location/thigh}
   #:db{:ident :tattoo.location/chest}
   #:db{:ident :tattoo.location/back}
   #:db{:ident :tattoo.location/stomache}
   #:db{:ident :tattoo.location/neck}
   #:db{:ident :tattoo.location/hand}
   #:db{:ident :tattoo.location/foot}
   #:db{:ident :tattoo.location/hip}

   #:db{:ident :tattoo.style/neotraditional}
   #:db{:ident :tattoo.style/newschool}
   #:db{:ident :tattoo.style/linework}
   #:db{:ident :tattoo.style/lettering}
   #:db{:ident :tattoo.style/black-and-white}

   ;; Tattoo
   {:db/ident :tattoo/uuid
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/size
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/location
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :tattoo/style
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :tattoo/owner
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/time-tattooing
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc "Time taken to finish the tattoo."}

   {:db/ident :tattoo/time-designing
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/charged
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one
    :db/doc "Amount charged from the client."}

    ;; Tattoo work experience
   {:db/ident :tattoo/experience-tattooing
    :db/valueType :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/experience-designing
    :db/valueType :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/experience-with-client
    :db/valueType :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident :tattoo/experience-would-do-again
    :db/valueType :db.type/number
    :db/cardinality :db.cardinality/one}])

(def cfg
  {:store {:backend :file :path "db/project_ink_v2_dev"}})

(defstate conn
  :start  (fn []
            (try
              (println "Create a database...")
              (d/create-database cfg :initial-tx schema)
              (catch Throwable t
                (println (.getMessage t))))
            (d/connect cfg))
  :stop (d/release conn))



(comment
  (d/release conn)
  (d/delete-database cfg)
  (d/create-database cfg :initial-tx schema)
  (d/transact conn schema)
  (export-db @conn "./db/dump"))

(def pull-pattern-tattoo '[* {:tattoo/size [*] :tattoo/location [*]}])


;; Database queries


(defn insert-tattoo
  "Insert new tattoo. Generate new UUID and return inserted values."
  ([tattoo] (insert-tattoo conn tattoo))
  ([conn tattoo]
   (let [tattoo-uuid      (java.util.UUID/randomUUID)
         tattoo-with-uuid (merge tattoo {:tattoo/uuid tattoo-uuid})
         tx-result        (d/transact conn [tattoo-with-uuid])]
     (d/pull (:db-after tx-result) '[*] [:tattoo/uuid tattoo-uuid]))))

(defn update-tattoo
  "Update existing tattoo and return inserted values."
  ([tattoo] (update-tattoo conn tattoo))
  ([conn tattoo]
   (let [tx-result        (d/transact conn [tattoo])]
     (d/pull (:db-after tx-result)
             pull-pattern-tattoo
             [:tattoo/uuid (:tattoo/uuid tattoo)]))))

(defn get-tattoo-by-uuid
  "Get tattoo by uuid."
  ([uuid] (get-tattoo-by-uuid conn uuid))
  ([conn uuid]
   (d/q '[:find (pull ?tattoo [*]) .
          :in $ ?uuid
          :where
          [?tattoo :tattoo/uuid ?uuid]]
        @conn
        uuid)))

(defn get-tattoos
  "Get list of tattoos."
  ([] (get-tattoos conn))
  ([conn]
   (d/q '[:find [(pull ?t [*]) ...]
          :in $
          :where
          [?t :tattoo/uuid _]]
          @conn)))


;; Sketces


(comment
  (def t-uuid
    (java.util.UUID/fromString "fc0a2768-c8de-425d-a7c3-16b79c720cf6"))

  (def temp-uuid (java.util.UUID/fromString "67a33ff2-8f75-4091-9956-0ed5180e3beb"))
  (update-tattoo conn
                 {:tattoo/uuid temp-uuid
                  :tattoo/size :tattoo-size/large
                  :tattoo/location :tattoo-location/ankle})


  ;; datahike instants need to be java.util.Date


  (let [t-uuid
        (java.util.UUID/randomUUID)]
    (d/transact conn [#:tattoo{:uuid        t-uuid
                               :title       "test-2"
                               :size        :tattoo.size/small
                               :location    :tattoo.location/hand
                               :date        (java.util.Date.)
                               :description "textual description."}]))

  (time/instant? (java.util.Date.))
  (time/instant? (time/instant))

  (def example-tattoo #:tattoo{:title "test" :uuid t-uuid})

  (insert-tattoo conn example-tattoo)

  (get-tattoo-by-uuid conn temp-uuid)

  (d/q '[:find [;;(pull ?t [* {:tattoo/size [*] :tattoo/location [*]}])

                (pull ?b [*])]
         :in $ ?title
         :where
         [?t :tattoo/title ?title]
         [?b :bill/tattoo ?t]]
       @conn
       "test")

  (d/q '[:find ?t
         :where
         [?t :tattoo/title "test"]]
       @conn)

  ;; blog -> pull just a once -> not wrapped in array
  (d/q '[:find (pull ?b [*]) .
         :where
         [?t :tattoo/title "test"]
         [?b :bill/amount _]]
       @conn))
