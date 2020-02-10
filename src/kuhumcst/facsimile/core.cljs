(ns kuhumcst.facsimile.core
  (:require [reagent.core :as r]
            [kuhumcst.facsimile.parse :as parse]
            [kuhumcst.facsimile.interop :as interop]))

;; Hiccup content injected into the shadow DOM of the shadow-content component.
;; This layer of indirection ensures that the same JS class can be used for
;; all uses of the shadow-content component.
(defonce content-registry
  (atom {}))

(def shadow-props
  {:connectedCallback        (fn [this]
                               (let [k         (.getAttribute this "data-key")
                                     content   (get @content-registry k)
                                     component #(fn [] content)]
                                 (r/render [component] (.-shadow this))))
   :disconnectedCallback     (fn [this] (println "disconnected: " this))
   :attributeChangedCallback (fn [this] (println "attribute changed: " this))})

(defn- shadow-constructor
  "Convert the HTML element into a shadow container, attaching a shadow root."
  [this]
  (set! (.-shadow this) (.attachShadow this #js{:mode "open"})))

(defn shadow-content
  "Render `hiccup` inside a shadow DOM together with scoped `css`."
  [hiccup css]
  (let [content [:<> [:style css] hiccup]
        k       (str (hash content))
        tags    (->> (parse/select-all hiccup)
                     (map first)
                     (set))]
    ;; Persist the actual content in our registry for later indirect rendering.
    (swap! content-registry assoc k content)

    ;; Define a custom HTML element that will render the hiccup content inside
    ;; its shadow DOM once connected to the light DOM.
    (interop/define "shadow-content" shadow-constructor shadow-props)

    ;; Define custom HTML elements for all of the hiccup vectors in the hiccup.
    (doseq [tag tags]
      (interop/define (name tag) :no-op {}))
    [:shadow-content {:data-key k}]))
