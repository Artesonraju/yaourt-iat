(ns iatrf-cljs.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(def init-data
  {:conf/factors
    [
      {:conf/factor "Personnel"
       :color 0xFFF
       :categories
        [
          {:conf/category "Moi" :targets ["je" "moi" "personellement"]}
          {:conf/category "Autrui" :targets ["eux" "ils" "les autres"]}
        ]
      }
      {:conf/factor "Pouvoir"
       :color 0xFFF
       :categories
        [
          {:conf/category "Dominance" :targets ["fort" "pouvoir" "supérieur"]}
          {:conf/category "Soumission" :targets ["faible" "inférieur" "soumis"]}
        ]
      }
     ]
   :conf/pages
    [
      ;{:id 0 :type :conf/start}
      {:id 1 :type :page/instruction :text "1. Appuyer sur e et i le plus rapidement possible"}
      ;{:id 2 :type :page/block 
      ; :label "Personnel seulement"
      ; :times 1
      ; :factors [{:factor {:conf/factor "Personnel"}:random false}]
      ;}
      {:id 3 :type :page/instruction :text "2. Appuyer sur i et e le plus rapidement possible"}
      {:id 4 :type :page/block
       :label "Pouvoir seulement"
       :times 2
       :factors [{:factor {:conf/factor "Pouvoir"} :random true}]
      }
      {:id 5 :type :page/instruction :text "3. Appuyer sur i et e le plus rapidement possible"}
      {:id 6 :type :page/block
       :label "Personnel et pouvoir"
       :times 1
       :factors 
        [
          {:factor {:conf/factor "Personnel"} :random false}
          {:factor {:conf/factor "Pouvoir"} :random true}
        ]
      }
      ;{:id 0 :type :page/end}
    ]
   :state/page-index 0
   :state/block {:sequence nil :phase nil}
   })

;; -----------------------------------------------------------------------------
;; Parsing

(defmulti read om/dispatch)

(defmethod read :state/page-index
  [{:keys [state]} k _]
  (let [st @state]
    {:value (st k)}))

(defmethod read :conf/pages
  [{:keys [state]} k _]
  (let [st @state]
    {:value (into [] (map #(get-in st %)) (get st k))}))

(defmulti mutate om/dispatch)

(defmethod mutate 'page/next
  [{:keys [state]} _ _]
  {:action
   (fn []
     (swap! state update-in
       [:state/page-index]
       inc)
     (println state))})

;; -----------------------------------------------------------------------------
;; Components

(defui Instruction
  static om/IQuery
  (query [this]
    [:id :type :text])
  Object
  (render [this]
    (let [{:keys [text] :as props} (om/props this)]
      (dom/div
        #js {:onClick
          (fn [e]
            ;;(when (== (.-which e) 32)
              (om/transact! this
                `[(page/next)]))}
        (dom/h1 nil "Consigne")
        (dom/p nil text)))))

(def instruction (om/factory Instruction))

(defui Block
  static om/IQuery
  (query [this]
    [:id :type :label])
  Object
  (render [this]
    (let [{:keys [label] :as props} (om/props this)]
      (dom/div
        #js {:onKeyPress
          (fn [e]
            (when (== (.-which e) 32)
              (om/transact! this
                `[(page/next ~props)])))}
        (dom/h1 nil label)))))

(def block (om/factory Block))

(defui Page
  static om/Ident
  (ident [this {:keys [id type]}]
    [type id])
  static om/IQuery
  (query [this]
    {:page/instruction (om/get-query Instruction) :page/block (om/get-query Block)})
  Object
  (render [this]
    (let [{:keys [id type favorites] :as props} (om/props this)]
      (dom/div
        #js {:style #js {:padding 10 :borderBottom "1px solid black"}}
        (dom/div nil
          (({:page/instruction    instruction
             :page/block          block} type)
            (om/props this)))))))

(def page (om/factory Page))

(defui RootView
  static om/IQuery
  (query [this]
    [:state/page-index {:conf/pages (om/get-query Page)}])
  Object
  (render [this]
    (println "Render RootView")
    (let [{:keys [state/page-index conf/pages]} (om/props this)]
      (dom/div nil
        (page (pages page-index))))))

(def reconciler
  (om/reconciler
    {:state  init-data
     :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler
  RootView (gdom/getElement "app"))