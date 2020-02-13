(ns user
  (:require [shadow.resource :as resource]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [kuhumcst.facsimile.parse :as parse]
            [kuhumcst.facsimile.style :as style]
            [kuhumcst.facsimile.core :as facsimile]))

(def tei-example
  (resource/inline "examples/tei/1151anno-anno-tei.xml"))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def attr-kmap
  {:xml:lang :lang
   :xml:id   :id})

;; TODO: shadow-dom content for tree-like structures, e.g. list and item
(defn app
  []
  (let [initial-hiccup (parse/xml->hiccup tei-example)
        prefix         (name (first initial-hiccup))
        hiccup         (parse/patch-hiccup initial-hiccup prefix attr-kmap)
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
      [facsimile/shadow-content hiccup css]]
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
