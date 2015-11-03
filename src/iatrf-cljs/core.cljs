(ns iatrf-cljs.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

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
       :color "0xFFF"
       :categories {
        "moi" #{"je"}
        "autrui" #{"eux"}
        }
      }
      "pouvoir"
      {:random true
       :color "0xFFF"
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

(defn build-fixed
  [name factor]
  (apply concat (for [[k s] (factor :categories)] 
    (map #(identity { :value %
                      :right [(factor :right)]
                      :left [(factor :left)]
                      :category k
                      :factor name
                    }) s))))

(defn random-left-right
  [length categories]
  (let [items [{:left [(first categories)] :right [(second categories)]}
               {:right [(first categories)] :left [(second categories)]}]]
    (if (even? length)
      (shuffle (mapcat #(repeat (/ length 2) %) items))
      (rest (shuffle (mapcat #(repeat (+ (/ length 2) 1) %) items))))))

(defn build-random
  [name factor]
  (let [categories (keys (factor :categories))]
    (apply concat (for [[k s] (factor :categories)]
      (let [random (random-left-right (count s) categories)
            m (zipmap s random)]
        (for [[w r] m] 
            (merge r { :value w
                       :category k
                       :factor name
                     })))))))

(defn add-fixed
  [factor steps]
  (map (fn [s]
    (-> s
      (update :left #(conj % (factor :left)))
      (update :right #(conj % (factor :right))))) steps))

(defn add-random
  [factor steps]
  (let [ categories (keys (factor :categories))
         random (random-left-right (count steps) categories)
        m (zipmap steps random)]
    (for [[s r] m] 
            (merge-with #(into [] (concat %1 %2)) s r))))

(defn add-factors [factors current steps]
  (let [other (first (vals (dissoc factors current)))]
    (cond
      (nil? other) steps
      (not (other :random)) (add-fixed other steps)
      :else (add-random other steps))))

(defn add-expected [step]
  (assoc step :expected
    (if (some #(= (step :category) %) (step :right))
      :right
      :left)))

(defn build-factor
  [factors name factor]
  (->> (if (not (factor :random))
        (build-fixed name factor)
        (build-random name factor))
      (add-factors factors name)
      (map add-expected)))

(defn add-ids
  [coll]
  (map-indexed (fn [id v] (assoc v :id id)) coll))

(defn build-block
  [factors block]
  (let [fs (select-keys factors (block :factors))
        steps (shuffle (apply concat (for [[n v] fs] (build-factor fs n v))))]
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
      (dom/div nil
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
      (dom/div nil
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
      (dom/div nil
        (dom/h1 nil "Consigne")
        (dom/p nil text)
        (dom/p nil "Appuyez 'Espace' pour continuer")))))

(def instruction (om/factory Instruction))

(defui Label
  static om/IQuery
  (query [this]
    [:id :value :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [left right] :as props} (om/props this)]
      (dom/div nil
        (dom/h1 nil (str left right))))))

(def label (om/factory Label))

(defui Cross
  static om/IQuery
  (query [this]
    [:id :value :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [left right] :as props} (om/props this)]
      (dom/div nil
        (dom/h1 nil (str left "+" right))))))

(def cross (om/factory Cross))

(defui Target
  static om/IQuery
  (query [this]
    [:id :value :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [left right value] :as props} (om/props this)]
      (dom/div nil
        (dom/h1 nil (str left value right))))))

(def target (om/factory Target))

(defui Wrong
  static om/IQuery
  (query [this]
    [:id :value :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [left right value] :as props} (om/props this)]
      (dom/div nil
        (dom/h1 nil (str left value " x" right))))))

(def wrong (om/factory Wrong))

(defui Transition
  static om/IQuery
  (query [this]
    [:id :value :type :left :right :category :factor :expected])
  Object
  (render [this]
    (let [{:keys [value] :as props} (om/props this)]
      (dom/h1 nil "-"))))

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

(defn next-page [state]
  (update-in state [:page/current] inc))

(defn set-timeout [ms]
  (js/setTimeout
        (fn [] (om/transact! (om/class->any reconciler RootView) `[(time/out)]))
        ms))

(defmulti dispatch-click #(identity [(% :type)]))

(defmethod dispatch-click [:page/intro]
  [page state keycode]
  (if (== (get-in conf [:keys :instruction]) keycode)
    (next-page state)
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
  (if (= (page :expected) side)
    (do 
      (set-timeout (get-in conf [:times :transition]))
      (next-page (next-page state)))
    (next-page state)))

(defmethod dispatch-click [:page/target]
  [page state keycode]
  (condp = keycode
    (get-in conf [:keys :left]) (target-answer :left page state)
    (get-in conf [:keys :right]) (target-answer :right page state)
    state))

(defn wrong-answer
  [side page state]
  (println side page)
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
  (next-page state))

(defmethod dispatch-timeout [:page/transition]
  [page state]
  (set-timeout (get-in conf [:times :label]))
  (next-page state))

(defn manage-timeout [state]
  (let [ [type id] ((state :page/pages) (state :page/current))
         page (get-in state [type id])]
    (dispatch-timeout page state)))

(defmethod mutate 'time/out
  [{:keys [state]} _ _]
  {:action
   (fn []
     (swap! state #(manage-timeout %)))})
