(ns iatrf-cljs.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [testdouble.cljs.csv :as csv]))

(enable-console-print!)

(def conf
    {:keys {
      :instruction 32
      :left 69
      :right 73
      }
     :times {
      :label 1000
      :cross 500
      :transition 250
      }
     :factors {
      "personnel"
      {:random false
       :color "#4F5"
       :categories {
        "moi" #{"je"}
        "autrui" #{"eux"}
        }
      }
      "pouvoir"
      {:random true
       :color "#FFF"
       :categories {
        "dominance" #{"fort"}
        "soumission" #{"faible"}
       }
      }
    }
   :intro "Merci de nous accorder un peu de votre temps."
   :end "Et voilà, c'est fini."
   :blocks
    [
      {:instruction "3. Appuyer sur e et i le plus rapidement possible"
       :label "Personnel et pouvoir"
       :factors ["personnel" "pouvoir"]
      }
      {:instruction "1. Appuyer sur e et i le plus rapidement possible"
       :label "personnel"
       :factors ["personnel"]
      }
      {:instruction "2. Appuyer sur e et i le plus rapidement possible"
       :label "pouvoir"
       :factors ["pouvoir"]
      }
    ]
  })

;; -----------------------------------------------------------------------------
;; Deriving conf to data

(defn fixed-left-right
  [length factor]
  (repeat length {:left [(factor :left)] :right [(factor :right)]}))

(defn random-left-right
  [length categories]
  (let [items [{:left [(first categories)] :right [(second categories)]}
               {:right [(first categories)] :left [(second categories)]}]]
    (if (even? length)
      (shuffle (mapcat #(repeat (/ length 2) %) items))
      (rest (shuffle (mapcat #(repeat (+ (/ length 2) 1) %) items))))))

(defn shuffle-left-right
  [length factor]
  (if (factor :random)
    (random-left-right length (keys (factor :categories)))
    (fixed-left-right length factor)))

(defn assoc-left-right
  [fs bfs steps]
  (let [left-rights (map #(shuffle-left-right (count steps) (fs %)) bfs)
        merged (apply map (partial merge-with concat) left-rights)]
    (map merge steps merged)))

(defn build-words [factors name]
  (let [factor (factors name)
        colors (map #(% :color) (vals factors))
        categories (keys (factor :categories))]
    (apply concat (for [[k s] (factor :categories)]
        (map #(identity {:colors colors
                         :target %
                         :category k
                         :factor name}) s)))))

(defn assoc-expected [step]
  (assoc step :expected
    (if (some #(= (step :category) %) (step :right))
      :right
      :left)))
  
(defn build-factor
  [factors name block-factors]
  (->> (build-words factors name)
       (assoc-left-right factors block-factors)
       (map assoc-expected)))

(defn add-ids
  [coll]
  (map-indexed (fn [id v] (assoc v :id id)) coll))

(defn build-block
  [factors block]
  (let [block-factors (block :factors)
        fs (select-keys factors block-factors)
        steps (shuffle (apply concat (for [[n _] fs] (build-factor fs n block-factors))))]
    (concat [{:type :page/instruction :text (block :instruction)}]
                      (mapcat (juxt #(assoc % :type :page/label)
                                    #(assoc % :type :page/cross)
                                    #(assoc % :type :page/target)
                                    #(assoc % :type :page/wrong)
                                    #(assoc % :type :page/transition))
                                    steps))))

(defn fix-factors
  [factors]
  (into {} (for [[k v] factors]
             (if (v :random)
               [k v]
               [k (merge v (zipmap (shuffle [:left :right]) (keys (v :categories))))]))))

(defn init-data
  [conf]
  (let [factors (fix-factors (conf :factors))
        steps (mapcat (partial build-block factors) (conf :blocks))
        intro {:type :page/intro :text (conf :intro)}
        end {:type :page/end :text (conf :end)}
        pages (into [] (add-ids (concat [intro] steps [end])))]
    identity {:page/pages pages
              :page/current 0}))

;; -----------------------------------------------------------------------------
;; Parsing

(defmulti read om/dispatch)

(defmethod read :page/current
  [{:keys [state]} k _]
  (let [st @state]
    {:value (st k)}))

(defmethod read :page/pages
  [{:keys [state]} k _]
  (let [st @state]
    {:value (into [] (map #(get-in st %)) (get st k))}))

(defmulti mutate om/dispatch)

;; -----------------------------------------------------------------------------
;; Components

(def reconciler
  (om/reconciler
    {:state  (init-data conf)
     :parser (om/parser {:read read :mutate mutate})}))

(defui Intro
  static om/IQuery
  (query [this]
    [:id :type :text])
  Object
  (render [this]
    (let [{:keys [text] :as props} (om/props this)]
      (dom/div #js {:className "centred"}
        (dom/h1 nil "Bienvenue !")
        (dom/p nil text)
        (dom/p nil "Appuyez 'Espace' pour continuer")))))

(def intro (om/factory Intro))

(defui End
  static om/IQuery
  (query [this]
    [:id :type :text])
  Object
  (render [this]
    (let [{:keys [text] :as props} (om/props this)]
      (dom/div #js {:className "centred"}
        (dom/h1 nil "Merci !")
        (dom/p nil text)))))

(def end (om/factory End))

(defui Instruction
  static om/IQuery
  (query [this]
    [:id :type :text])
  Object
  (render [this]
    (let [{:keys [text] :as props} (om/props this)]
      (dom/div #js {:className "centred"}
        (dom/h1 nil "Consigne")
        (dom/p nil text)
        (dom/p nil "Appuyez 'Espace' pour continuer")))))

(def instruction (om/factory Instruction))

(defui Label
  static om/IQuery
  (query [this]
    [:id :target :type :left :right :category :factor :expected :colors])
  Object
  (render [this]
    (let [{:keys [left right colors] :as props} (om/props this)]
      (dom/div nil
        (dom/div #js {:className "left"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors left))
        (dom/div #js {:className "right"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors right))))))

(def label (om/factory Label))

(defui Cross
  static om/IQuery
  (query [this]
    [:id :target :type :left :right :category :factor :expected :colors])
  Object
  (render [this]
    (let [{:keys [left right colors] :as props} (om/props this)]
      (dom/div nil
        (dom/div #js {:className "left"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors left))
        (dom/div #js {:className "right"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors right))
        (dom/div #js {:className "centred"}
          (dom/p #js {:className "item"} "+"))))))

(def cross (om/factory Cross))

(defui Target
  static om/IQuery
  (query [this]
    [:id :target :type :left :right :category :factor :expected :colors])
  Object
  (render [this]
    (let [{:keys [left right target colors] :as props} (om/props this)]
      (dom/div nil
        (dom/div #js {:className "left"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors left))
        (dom/div #js {:className "right"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors right))
        (dom/div #js {:className "centred"}
          (dom/p #js {:className "item"} target))))))

(def target (om/factory Target))

(defui Wrong
  static om/IQuery
  (query [this]
    [:id :target :type :left :right :category :factor :expected :colors])
  Object
  (render [this]
    (let [{:keys [left right target colors] :as props} (om/props this)]
      (dom/div nil
        (dom/div #js {:className "left"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors left))
        (dom/div #js {:className "right"}
          (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors right))
        (dom/div #js {:className "centred"}
          (dom/p #js {:className "item"} target))
        (dom/div #js {:className "top-centred"}
          (dom/p #js {:className "item" :style #js {:color "#f00"}} "X"))))))

(def wrong (om/factory Wrong))

(defui Transition
  static om/IQuery
  (query [this]
    [:id :target :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [target] :as props} (om/props this)]
      (dom/h1 nil nil))))

(def transition (om/factory Transition))

(defui Page
  static om/Ident
  (ident [this {:keys [id type]}]
    [type id])
  static om/IQuery
  (query [this]
    {:page/intro (om/get-query Intro)
     :page/instruction (om/get-query Instruction)
     :page/label (om/get-query Label)
     :page/cross (om/get-query Cross)
     :page/target (om/get-query Target)
     :page/wrong (om/get-query Wrong)
     :page/transition (om/get-query Transition)
     :page/end (om/get-query End)})
  Object
  (render [this]
    (let [{:keys [id type favorites] :as props} (om/props this)]
      (dom/div
        nil
        (dom/div nil
          (({:page/intro          intro
             :page/instruction    instruction
             :page/label          label
             :page/cross          cross
             :page/target         target
             :page/wrong          wrong
             :page/transition     transition
             :page/end            end} type)
            (om/props this)))))))

(def page (om/factory Page))

(defui RootView
  static om/IQuery
  (query [this]
    [:page/current {:page/pages (om/get-query Page)}])
  Object
  (render [this]
    (println "Render RootView")
    (let [{:keys [page/current page/pages]} (om/props this)]
      (dom/div nil
        (page (pages current))))))

(defonce key-listener
  (.addEventListener
    js/document
    "keydown" 
    (fn [e]
      ()
      (om/transact! (om/class->any reconciler RootView)
        `[(user/click ~{:keycode (.-which e)})]))))

(om/add-root! reconciler
  RootView (gdom/getElement "app"))

;; -----------------------------------------------------------------------------
;; Dispatch

(defn init-result [state]
  (assoc state :result [["left" "right" "target" "factor" "category" "expected" "response" "time"]]))

(defn next-page [state]
  (update-in state [:page/current] inc))

(defn start-timer [state]
  (assoc state :timer (.getTime (js/Date.))))

(defn set-timeout [ms]
  (js/setTimeout
        (fn [] (om/transact! (om/class->any reconciler RootView) `[(time/out)]))
        ms))

(defmulti dispatch-click #(identity [(% :type)]))

(defmethod dispatch-click [:page/intro]
  [page state keycode]
  (if (== (get-in conf [:keys :instruction]) keycode)
    (init-result (next-page state))
    state))

(defmethod dispatch-click [:page/instruction]
  [page state keycode]
  (if (== (get-in conf [:keys :instruction]) keycode)
    (do
      (set-timeout (get-in conf [:times :label]))
      (next-page state))
    state))

(defn target-answer
  [side page state]
  (let [response-time (- (.getTime (js/Date.)) (state :timer))
        result (= (page :expected) side)
        s (update
            state
            :result
            #(concat % [[(page :left)
                         (page :right)
                         (page :target)
                         (page :factor)
                         (page :category)
                         (page :expected)
                         result
                         response-time]]))]
    (if result
      (do 
        (set-timeout (get-in conf [:times :transition]))
        (next-page (next-page s)))
      (next-page s))))

(defmethod dispatch-click [:page/target]
  [page state keycode]
  (condp = keycode
    (get-in conf [:keys :left]) (target-answer :left page state)
    (get-in conf [:keys :right]) (target-answer :right page state)
    state))

(defn wrong-answer
  [side page state]
  (if (= (page :expected) side)
    (do 
      (set-timeout (get-in conf [:times :transition]))
      (next-page state))
    state))
  
(defmethod dispatch-click [:page/wrong]
  [page state keycode]
  (condp = keycode
    (get-in conf [:keys :left]) (wrong-answer :left page state)
    (get-in conf [:keys :right]) (wrong-answer :right page state)
    state))

(defmethod dispatch-click :default
  [_ state _]
  state)

(defn manage-click [state params]
  (let [ keycode (params :keycode)
         [type id] ((state :page/pages) (state :page/current))
         page (get-in state [type id])]
    (dispatch-click page state keycode)))

(defmethod mutate 'user/click
  [{:keys [state]} _ params]
  {:action
   (fn []
     (swap! state #(manage-click % params)))})

(defmulti dispatch-timeout #(identity [(% :type)]))

(defmethod dispatch-timeout [:page/label]
  [page state]
  (set-timeout (get-in conf [:times :cross]))
  (next-page state))

(defmethod dispatch-timeout [:page/cross]
  [_ state]
  (start-timer (next-page state)))

(defmethod dispatch-timeout [:page/transition]
  [page state]
  (set-timeout (get-in conf [:times :label]))
  (next-page state))

(defmethod dispatch-timeout [:page/end]
  [page state]
  (println (state :result))
  state)

(defn manage-timeout [state]
  (let [ [type id] ((state :page/pages) (state :page/current))
         page (get-in state [type id])]
    (dispatch-timeout page state)))

(defmethod mutate 'time/out
  [{:keys [state]} _ _]
  {:action
   (fn []
     (swap! state #(manage-timeout %)))})
