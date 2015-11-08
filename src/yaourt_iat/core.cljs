(ns yaourt-iat.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [yaourt-iat.util :as util]
            [yaourt-iat.conf :refer [conf]]
            [om.dom :as dom]))

(enable-console-print!)

(defmulti read om/dispatch)

(defmethod read :page/current
  [{:keys [state] :as env} _ {:keys [remote?]}]
  (let [st @state]
    (if-let [v (get st :page/current)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :page/pages
  [{:keys [state] :as env} _ {:keys [remote?]}]
  (let [st @state]
    (if-let [v (get st :page/pages)]
      {:value (into [] (map #(get-in st %)) v) :remote true}
      {:remote true})))

(defmulti mutate om/dispatch)

;; -----------------------------------------------------------------------------
;; Reconciler

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state {}
     :parser parser
     :send (util/transit-conf)
     :history 0}))

;; -----------------------------------------------------------------------------
;; Components

(defui Word
  static om/IQuery
  (query [this]
    [:type :value])
  Object
  (render [this]
    (let [[_ value] (om/props this)]
      (dom/p #js {:className "item"} value))))

(def word (om/factory Word))

(defui Image
  static om/IQuery
  (query [this]
    [:type :value])
  Object
  (render [this]
    (let [[_ value] (om/props this)]
        (dom/img
          #js {:src value}))))

(def image (om/factory Image))

(defui Item
  static om/Ident
  (ident [this {:keys [type value]}]
    [type value])
  static om/IQuery
  (query [this]
    {:target/word (om/get-query Word)
     :target/image (om/get-query Image)
     })
  Object
  (render [this]
    (let [[type value] (om/props this)]
        (dom/div nil
          (({:target/word   word
             :target/image  image
            } type)
            (om/props this))))))

(def item (om/factory Item))

(defui ImageLoader
  static om/Ident
  (ident [this {:keys [type value]}]
    [type value])
  static om/IQuery
  (query [this]
    [:type :value])
  Object
  (render [this]
    (let [{:keys [value]} (om/props this)]
      (dom/img #js {:src value
                    :className "invisible"
                    :onLoad #(om/transact! (om/ref->any reconciler [:page/intro 0]) `[(img/loaded)])
                    :onError #(om/transact! (om/ref->any reconciler [:page/intro 0]) `[(img/error)])}))))

(def image-loader (om/factory ImageLoader))

(defui Intro
  static om/IQuery
  (query [this]
    [:id :type :text :error :img-count {:loader {:target/image (om/get-query ImageLoader)}}])
  Object
  (render [this]
    (let [{:keys [text loader error img-count loader] :as props} (om/props this)
          img-total (count loader)]
      (dom/div #js {:className "centred"}
        (dom/h1 nil "Bienvenue !")
        (dom/p nil text)
        (map image-loader loader)
        (cond
          error (dom/p nil "Erreur dans le chargement des images.")
          (< img-count img-total) (dom/p nil (str "Veuillez patienter. Chargement des images : " img-count "/" img-total))
          :else (dom/p nil "Appuyer sur Espace pour continuer")
          )))))

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
          (map #(dom/p nil %) text)))))

(def instruction (om/factory Instruction))

(defn render-categories
  [class items colors]
  (dom/div #js {:className class}
    (map #(dom/p #js {:className "item" :style #js {:color %1}} %2) colors items)))

(defui Label
  static om/IQuery
  (query [this]
    [:id :type :left :right :colors])
  Object
  (render [this]
    (let [{:keys [left right colors] :as props} (om/props this)]
      (dom/div nil
        (render-categories "left" left colors)
        (render-categories "right" right colors)))))

(def label (om/factory Label))

(defui Cross
  static om/IQuery
  (query [this]
    [:id :type :left :right :colors])
  Object
  (render [this]
    (let [{:keys [left right colors] :as props} (om/props this)]
      (dom/div nil
        (render-categories "left" left colors)
        (render-categories "right" right colors)
        (dom/div #js {:className "centred"}
          (dom/p #js {:className "item"} "+"))))))

(def cross (om/factory Cross))

(defui Target
  static om/IQuery
  (query [this]
    [:id :type :left :right :category :factor :expected :colors :color {:target (om/get-query Item)}])
  Object
  (render [this]
    (let [{:keys [left right target colors color] :as props} (om/props this)]
      (dom/div nil
        (render-categories "left" left colors)
        (render-categories "right" right colors)
        (dom/div #js {:className "centred" :style #js {:color color}}
          (item target))))))

(def target (om/factory Target))

(defui Wrong
  static om/IQuery
  (query [this]
    [:id :type :left :right :colors :color :expected {:target (om/get-query Item)}])
  Object
  (render [this]
    (let [{:keys [left right target colors color] :as props} (om/props this)]
      (dom/div nil
        (render-categories "left" left colors)
        (render-categories "right" right colors)
        (dom/div #js {:className "centred" :style #js {:color color}}
          (item target))
        (dom/div #js {:className "top-centred"}
          (dom/p #js {:className "item" :style #js {:color "#f00"}} "X"))))))

(def wrong (om/factory Wrong))

(defui Transition
  static om/IQuery
  (query [this]
    [:id :type])
  Object
  (render [this]
      (dom/h1 nil nil)))

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
    (let [{:keys [id type] :as props} (om/props this)]
        (dom/div nil
          (({:page/intro          intro
             :page/instruction    instruction
             :page/label          label
             :page/cross          cross
             :page/target         target
             :page/wrong          wrong
             :page/transition     transition
             :page/end            end} type)
            (om/props this))))))

(def page (om/factory Page))

(defui RootView
  static om/IQuery
  (query [this]
    [:page/current {:page/pages (om/get-query Page)}])
  Object
  (render [this]
    (let [{:keys [page/current page/pages]} (om/props this)]
      (dom/div nil
        (page (pages current))))))

(om/add-root! reconciler RootView (gdom/getElement "app"))

;; -----------------------------------------------------------------------------
;; Global listener

(defonce key-listener
  (.addEventListener
    js/document
    "keydown" 
    (fn [e]
      (om/transact! (om/class->any reconciler RootView)
        `[(user/click ~{:keycode (.-which e)})]))))

;; -----------------------------------------------------------------------------
;; Dispatch mutate

(defn init-result [state]
  (merge state {:results [["left" "right" "target" "factor" "category" "expected" "response" "time"]]
                :page/count (count (:page/pages state))}))

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
  (let [intro (get-in state [:page/intro 0])
        img-count (:img-count intro)
        img-total (count (:loader intro))]
    (if (and (== (get-in conf [:keys :instruction]) keycode) (>= img-count img-total))
      (init-result (next-page state))
      state)))

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
            :results
            #(concat % [[(into [] (page :left))
                         (into [] (page :right))
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
  (let [current (:page/current state)
        count (:page/count state)]
    (if (< current (- count 2))
      (set-timeout (get-in conf [:times :transition]))
      (set-timeout 0)))
  (next-page state))

(defmethod dispatch-timeout [:page/end]
  [page state]
  (util/transit-results (state :results))
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

(defn img-loaded
  [state]
  (update-in state [:page/intro 0 :img-count] inc))

(defn img-error
  [state]
  (assoc-in state [:page/intro 0 :error] true))

(defmethod mutate 'img/loaded
  [{:keys [state]} _ _]
  {:action
   (fn []
     (swap! state img-loaded))})

(defmethod mutate 'img/error
  [{:keys [state]} _ _]
  {:action
   (fn []
     (swap! state img-error))})