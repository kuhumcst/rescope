(ns user
  (:require [shadow.resource :as resource]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [meander.epsilon :as m]
            [kuhumcst.facsimile.parse :as parse]
            [kuhumcst.facsimile.select :as select]
            [kuhumcst.facsimile.style :as style]
            [kuhumcst.facsimile.core :as facsimile]))

(def tei-example
  ;(resource/inline "examples/tei/1151anno-anno-tei.xml"))
  (resource/inline "examples/tei/tei_example.xml"))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def attr-kmap
  {:xml:lang :lang
   :xml:id   :id})

(def da-type
  {"conference" "Konference"
   "org"        "Organisation"
   "pers"       "Person"
   "place"      "Sted"
   "publ"       "Publikation"
   "receiver"   "Modtager"
   "sender"     "Afsender"})

(def pred->comp
  {(comp #{:tei-list} first) (fn [this]
                               [:ul
                                (for [child (array-seq (.-children this))]
                                  [:li {:dangerouslySetInnerHTML {:__html (.-innerHTML child)}
                                        :key                     (hash child)}])])
   (comp :data-ref second)   (fn [this]
                               (let [dataset   (.-dataset this)
                                     href      (.-ref dataset)
                                     href-type (da-type (.-type dataset))]
                                 [:a {:href  href
                                      :title href-type}
                                  [:slot]]))})

(defn algorithmic-rewrite
  [node]
  (let [matching-comp #(fn [_ pred comp]
                         (when (pred %)
                           (reduced comp)))]
    (reduce-kv (matching-comp node) nil pred->comp)))

(defn meander-rewrite*
  [x]
  (m/rewrite x
    [:list (m/or {:as ?attr}
                 (m/let [?attr {}])) .
     [:item !x] ...]
    [:ul ?attr .
     [:li !x] ...]

    [_ {:ref  (m/some ?ref)
        :type ?type} & _]
    [:a {:href  ?ref
         :title (m/app da-type ?type)}
     [:slot]]))

(defn hiccup->comp
  [hiccup]
  (when hiccup
    (fn [this] hiccup)))

(def meander-rewrite
  (comp hiccup->comp meander-rewrite*))

(defn app
  []
  (let [initial-hiccup (parse/xml->hiccup tei-example)
        prefix         (name (first initial-hiccup))
        patch-hiccup   (parse/patch-fn prefix attr-kmap meander-rewrite)
        hiccup         (parse/transform patch-hiccup initial-hiccup)
        css            (style/patch-css css-example prefix)
        teiheader      (select/one hiccup (select/element :tei-teiheader))
        facsimile      (select/one hiccup (select/element :tei-facsimile))
        text           (select/one hiccup (select/element :tei-text))
        test-nodes     (select/all hiccup
                                   (select/element :tei-forename)
                                   (select/attr {:type "first"}))]
    [:<>
     [:fieldset
      [:legend "Document"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint hiccup))]]
      [:details
       [:summary "CSS"]
       [:pre css]]
      [facsimile/custom-html hiccup css]]
     [:fieldset
      [:legend "Header"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint teiheader))]]]
     [:fieldset
      [:legend "Facsimile"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint facsimile))]]]
     [:fieldset
      [:legend "Text"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint text))]]]
     [:fieldset
      [:legend "Test output"]
      [:pre (with-out-str (pprint test-nodes))]]]))

(def root
  (js/document.getElementById "app"))

(defn ^:dev/after-load render
  []
  (r/render [app] root))

(defn start-dev
  []
  (println "Started development environment for kuhumcst/facsimile.")
  (render))
