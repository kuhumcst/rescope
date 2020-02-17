(ns kuhumcst.facsimile.shadow
  "Reagent components for integrating with the shadow DOM."
  (:require [reagent.core :as r]))

;; Hickory keeps empty attrs around by default, but we shouldn't rely on that.
(defn- root+content
  [hiccup]
  (let [n (if (map? (second hiccup)) 2 1)]
    [(vec (take n hiccup))
     (vec (drop n hiccup))]))

(defn root
  "Get a :ref fn for a DOM element to render a given `comp` as its shadow root.
  The component should accept a single argument: the element's DOM reference."
  [comp]
  (fn [this]
    (when this
      (set! (.-shadow this) (.attachShadow this #js{:mode "open"}))
      (r/render [comp this] (.-shadow this)))))

(defn scoped
  "Render the content of `hiccup` in a shadow DOM together with scoped `css`.
  The root element of the hiccup tree becomes the shadow host."
  [hiccup css]
  (let [[[root-tag root-attr] content] (root+content hiccup)
        root-comp (fn [_]
                    (into [:<> [:style css]] content))]
    [root-tag (assoc root-attr :ref (root root-comp))]))
