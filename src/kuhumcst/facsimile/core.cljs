(ns kuhumcst.facsimile.core
  (:require [reagent.core :as r]))

(defn app
  []
  [:p "Hello, facsimile world!"])

(defn ^:dev/after-load render
  []
  (r/render [app] (js/document.getElementById "app")))

(defn start-dev
  []
  (println "Started development environment for kuhumcst/facsimile.")
  (render))
