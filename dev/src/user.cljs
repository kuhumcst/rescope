(ns user
  (:require [shadow.resource :as resource]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [kuhumcst.facsimile.xml :as xml]
            [kuhumcst.facsimile.style :as style]
            [kuhumcst.facsimile.core :as facsimile]))

(def tei-example
  (resource/inline "examples/tei/tei_example.xml"))

(def css-example
  (resource/inline "examples/css/tei.css"))

(defn app
  []
  (let [initial-xml (xml/hiccup-parse tei-example)
        root-tag    (name (first initial-xml))
        xml         (xml/preprocess initial-xml)
        css         (style/prefix-css root-tag css-example)
        teiheader   (xml/select xml (xml/element :tei-teiheader))
        facsimile   (xml/select xml (xml/element :tei-facsimile))
        text        (xml/select xml (xml/element :tei-text))
        test-nodes  (xml/select-all xml
                                    (xml/element :tei-forename)
                                    (xml/attr {:type "first"}))]
    [:<>
     [:fieldset
      [:legend "Document"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint xml))]]
      [:details
       [:summary "CSS"]
       [:pre css]]
      [facsimile/xml-shadow xml css]]
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
