(ns user
  (:require [shadow.resource :as resource]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [kuhumcst.facsimile.parse :as parse]
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

(def ref-type->da-str
  {"org"      "Organisation"
   "pers"     "Person"
   "place"    "Sted"
   "publ"     "Publikation"
   "receiver" "Modtager"
   "sender"   "Afsender"})

;; TODO: need an fn for converting this to the replacements* style below
(def pred->comp
  {[:list] (fn [this]
             [:ul
              (for [child (array-seq (.-children this))]
                [:li {:dangerouslySetInnerHTML {:__html (.-innerHTML child)}
                      :key                     (hash child)}])])})

(def pred->comp*
  {(comp #{:tei-list} first) (fn [this]
                               [:ul
                                (for [child (array-seq (.-children this))]
                                  [:li {:dangerouslySetInnerHTML {:__html (.-innerHTML child)}
                                        :key                     (hash child)}])])
   (comp :data-ref second)   (fn [this]
                               (let [dataset   (.-dataset this)
                                     href      (.-ref dataset)
                                     href-type (ref-type->da-str (.-type dataset))]
                                 [:a {:href  href
                                      :title href-type}
                                  [:slot]]))})

(defn app
  []
  (let [initial-hiccup (parse/xml->hiccup tei-example)
        prefix         (name (first initial-hiccup))
        patch-hiccup   (parse/patch-fn prefix attr-kmap pred->comp*)
        hiccup         (parse/transform patch-hiccup initial-hiccup)
        css            (style/patch-css css-example prefix)
        teiheader      (parse/select hiccup (parse/element :tei-teiheader))
        facsimile      (parse/select hiccup (parse/element :tei-facsimile))
        text           (parse/select hiccup (parse/element :tei-text))
        test-nodes     (parse/select-all hiccup
                                         (parse/element :tei-forename)
                                         (parse/attr {:type "first"}))]
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
