(ns user
  (:require [reagent.core :as r]
            [kuhumcst.facsimile.core :as facsimile]))

(defn app
  []
  [:p "Hello, facsimile world!"])

(def root
  (js/document.getElementById "app"))

(defn ^:dev/after-load render
  []
  (r/render [app] root))

(defn start-dev
  []
  (println "Started development environment for kuhumcst/facsimile.")
  (render))
