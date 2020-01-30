(ns user
  (:require [shadow.resource :as sr]
            [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [kuhumcst.facsimile.core :as facsimile]))

(def tei-example
  (sr/inline "tei_example.xml"))

(defn app
  []
  (let [xml-hiccup (facsimile/xml->hiccup tei-example)]
    [:<>
     [:details
      [:pre
       (with-out-str (pprint xml-hiccup))]]
     xml-hiccup]))

(def root
  (js/document.getElementById "app"))

(defn ^:dev/after-load render
  []
  (r/render [app] root))

(defn start-dev
  []
  (println "Started development environment for kuhumcst/facsimile.")
  (render))
