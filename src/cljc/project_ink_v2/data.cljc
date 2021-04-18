(ns project-ink-v2.data
  (:require [malli.core :as m]
            [malli.transform :as mt]))

(def tattoo-locations
  [:tattoo.location/ankle
   :tattoo.location/ribs
   :tattoo.location/shoulder
   :tattoo.location/forearm
   :tattoo.location/innerarm
   :tattoo.location/calf
   :tattoo.location/thigh
   :tattoo.location/chest
   :tattoo.location/back
   :tattoo.location/stomache
   :tattoo.location/neck
   :tattoo.location/hand
   :tattoo.location/foot
   :tattoo.location/hip])

(def tattoo-styles
  [:tattoo.style/neotraditional
   :tattoo.style/newschool
   :tattoo.style/linework
   :tattoo.style/lettering])

(def tattoo-style-options (map #(hash-map :key (keyword %) :display-name %) tattoo-styles))
(def tattoo-style-filters (concat [{:key :style-all :display-name "all styles"}]))
(def tattoo-size ["tiny", "small", "medium", "large", "humongous"])
(def TattooLocation (reduce conj [:enum ] tattoo-locations))
(def TattooStyle (reduce conj [:enum] tattoo-styles))
(def TattooSize (reduce conj [:enum] tattoo-size))
(def price-brackets
  (concat [{:key :price-range-all
            :min nil
            :max nil
            :display-name "all prices"}]
          (map #(hash-map :key (keyword (str "price-range-" % "-to-" (+ % 100)))
                          :min %
                          :max (+ % 100)
                          :display-name (str  % " - " (+ % 100)))
               (range 100 2000 100))))

;; ENUMS
(def OneToFive [:and int? [:>= 1] [:<= 5]])

(comment (def Tattoo
           [:map
            [:date string?]
            [:price [:and number? [:>= 0]]]
            [:work-tattooing number?]
            [:work-design number?]
            [:style TattooStyle]
            [:location TattooLocation]
            [:size TattooSize]
            [:experience-tattoo OneToFive]
            [:experience-design OneToFive]
            [:experience-client OneToFive]
            [:experience-would-do-again boolean?]]))


(def Tattoo
  [:map
   [:tattoo/title {:optional false} string?]
   [:tattoo/size  string?]
   [:tattoo/location  [:vector [:and keyword? TattooLocation]]]
   [:tattoo/date string?]
   [:tattoo/description string?]
   [:tattoo/style {:optional false} string?]])

(comment

  (m/validate Tattoo {:tattoo/location [:foobar]})
  (m/validate Tattoo {:tattoo/location [:hand]})

  (m/validate [:vector [:and keyword? [:enum :foo :bar]]] [:foo :bar])

  (m/validate
   [:map [:tattoo/location  [:vector [:enum :hand :foot]]]]
   {:tattoo/location [:foot]})

  (m/validate [:map [:foo [:vector [:and [:enum :hand] keyword?]]]] {:foo [":hand"]}))

(comment
  (def try-transform
    {:tattoo/location ["tattoo.location/hand"]
     :tattoo/title "This is a new name"})


  (m/validate Tattoo try-transform)

  (m/decode keyword? "foobar" (mt/string-transformer))

  (m/decode Tattoo try-transform (mt/string-transformer))
;; => #:tattoo{:location ":tattoo-location/hand", :title "This is a new name"}
  )
