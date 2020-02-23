(ns kuhumcst.facsimile.shadow
  "Reagent components for integrating with the shadow DOM."
  (:require [reagent.core :as r]
            [kuhumcst.facsimile.interop :as interop]))

;; TODO: validate and only define actual custom element tags
(defn define-custom-elements!
  "Define custom HTML elements covering all `tags`."
  [tags]
  (doseq [tag tags]
    (interop/define! tag)))

(defn root-ref
  "Get a :ref fn for a DOM element to render a given `comp` as its shadow root.
  The component should accept a single argument: the element's DOM reference."
  [comp]
  (fn [this]
    (when this
      (set! (.-shadow this) (.attachShadow this #js{:mode "open"}))
      (r/render [comp this] (.-shadow this)))))

(defn scope
  "Render `hiccup` inside a shadow DOM with the root element as the shadow host.
  Optionally accepts scoped `css` and `tags` to define as custom HTML elements."
  ([hiccup & [css tags]]
   (define-custom-elements! tags)
   (let [[[tag attr] children] (if (map? (second hiccup))
                                 (split-at 2 hiccup)
                                 (split-at 1 hiccup))
         comp (fn [_] (into [:<> (when css [:style css])] children))]
     [tag (assoc attr :ref (root-ref comp))])))
