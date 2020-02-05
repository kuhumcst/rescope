(ns kuhumcst.facsimile.core
  (:require [reagent.core :as r]
            [kuhumcst.facsimile.xml :as xml]
            [kuhumcst.facsimile.interop :as interop]))

;; Hiccup content injected into the shadow DOM of the `xml-shadow` component.
;; This layer of indirection ensures that the same JS class can be used for
;; all uses of `xml-shadow`.
(defonce xml-content
  (atom {}))

(def shadow-props
  {:connectedCallback        (fn [this]
                               (let [k         (.getAttribute this "data-key")
                                     content   (get @xml-content k)
                                     component #(fn [] content)]
                                 (r/render [component] (.-shadow this))))
   :disconnectedCallback     (fn [this] (println "disconnected: " this))
   :attributeChangedCallback (fn [this] (println "attribute changed: " this))})

(defn- shadow-constructor
  "Convert the HTML element into a shadow container, attaching a shadow root."
  [this]
  (set! (.-shadow this) (.attachShadow this #js{:mode "open"})))

(defn xml-shadow
  "Render parsed `xml` content inside a shadow DOM together with scoped `css`."
  [xml css]
  (let [content [:<> [:style css] xml]
        k       (str (hash content))
        tags    (->> (xml/select-all xml)
                     (map first)
                     (set))]
    ;; Persist the actual content in our registry for later indirect rendering.
    (swap! xml-content assoc k content)

    ;; Define a custom HTML component that will render the XML content inside
    ;; its shadow DOM once connected to the light DOM.
    (interop/define "xml-shadow" shadow-constructor shadow-props)

    ;; Define custom HTML components for all of the hiccup vectors in the XML.
    (doseq [tag tags]
      (interop/define (name tag) :no-op {}))
    [:xml-shadow {:data-key k}]))
