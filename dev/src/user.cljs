(ns user
  (:require [shadow.resource :as resource]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [kuhumcst.facsimile.xml :as xml]))

(def tei-example
  (resource/inline "tei_example.xml"))

(defn app
  []
  (let [xml        (xml/parse tei-example)
        teiheader  (xml/select xml (xml/element :tei-teiheader))
        facsimile  (xml/select xml (xml/element :tei-facsimile))
        text       (xml/select xml (xml/element :tei-text))
        test-nodes (xml/select-all xml
                                   (xml/element :tei-forename)
                                   (xml/attr {:type "first"}))]
    [:<>
     [:details
      [:summary "Full hiccup parse"]
      [:pre (with-out-str (pprint xml))]]
     [:fieldset
      [:legend "Header"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint teiheader))]]
      teiheader]
     [:fieldset
      [:legend "Facsimile"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint facsimile))]]
      facsimile]
     [:fieldset
      [:legend "Text"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint text))]]
      text]
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
