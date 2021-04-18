(ns project-ink-v2.db.core-test
  (:require
   [java-time :as time]
   [datahike.api :as d]
   [clojure.pprint :refer [pprint]]
   [project-ink-v2.db.core :as SUT]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [project-ink-v2.config :refer [env]]
   [mount.core :as mount]))

(comment (use-fixtures
           :once
           (fn [f]
             (mount/start
              #'project-ink-v2.config/env
              #'project-ink-v2.db.core/*db*)
             (migrations/migrate ["migrate"] (select-keys env [:database-url]))
             (f))))

(defn test-db
  "
  Create in memory datahike database for testing.
  If one exists already delete it and then re-create
  for the test.
  "
  []
  (let [db-uri  "datahike:mem://test"]
    (d/delete-database db-uri)
    (d/create-database db-uri)
    (let [conn (d/connect db-uri)]
      (d/transact conn SUT/schema)
      conn)))

(deftest tattoo-tests

  (testing "insert-tattoo")

  (let [conn (test-db)

        tattoo #:tattoo{:title       "test"
                        :size        :tattoo-size/small
                        :location    :tattoo-location/hand
                        :date        (java.util.Date.)
                        :description "textual description."
                        :style       :tattoo-style/neotraditional}

        _ (SUT/insert-tattoo conn tattoo)

        res (-> (d/q '[:find (pull ?e [* {:tattoo/size     [*]
                                          :tattoo/location [*]}])
                       :where
                       [?e :tattoo/title "test"]]
                     @conn)
                ffirst)]

    (pprint res)

    (is (= (:tattoo/title  res) "test"))
    (is (= (get-in res [:tattoo/size :db/ident]) :tattoo-size/small))
    (is (= (get-in res [:tattoo/location :db/ident]) :tattoo-location/hand))
    (is (time/instant? (-> res :tattoo/date .toInstant))))

  (testing "get tattoo by uuid")

  (let [conn   (test-db)
        uuid   (java.util.UUID/fromString "fc0a2768-c8de-425d-a7c3-16b79c720cf6")
        tattoo #:tattoo{:title "test get with uuid"
                        :uuid  uuid}
        _  (SUT/insert-tattoo conn tattoo)
        res (SUT/get-tattoo-by-uuid conn uuid)
        ]
    (pprint res)
    (is (= (:tattoo/uuid res) uuid))))
