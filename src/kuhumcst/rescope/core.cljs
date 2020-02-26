(ns kuhumcst.rescope.core
  "Reagent components for integrating with the shadow DOM."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [hickory.zip :as hzip]
            [reagent.core :as r]
            [kuhumcst.rescope.select :as select]
            [kuhumcst.rescope.interop :as interop]))

(defn- ignore?
  "Return true if it makes sense to ignore this loc."
  [[[tag attr & content :as node] _ :as loc]]
  (and (vector? node)
       (= tag :<>)))                                        ; React fragments

(defn postprocess
  "Apply `postprocessor` to every relevant loc of a zipper made from a `hiccup`
  tree and return the transformed structure."
  [postprocessor hiccup]
  (loop [loc (hzip/hiccup-zip hiccup)]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (if (ignore? loc)
                         loc
                         (postprocessor loc)))))))

;; The way hiccup is zipped, every branch is a hiccup vector, while everything
;; else is a leaf. Leafs are mostly string content, but can be other types too.
(defn postprocessor
  "Return an fn for editing a hiccup zipper with a `leaf-fn` and a `branch-fn`.
  Each fn should take a loc as their input and return an edited version.

  NOTE: all edits *must* preserve node metadata!"
  [leaf-fn branch-fn]
  (fn [[node _ :as loc]]
    (if (vector? node)
      (branch-fn loc)
      (leaf-fn loc))))

;; TODO: only return custom tags, not normal HTML tags
(defn hiccup->custom-tags
  "Get a set of all tags (as strings) found in a `hiccup` tree."
  [hiccup]
  (->> (select/all hiccup)
       (map (comp str/lower-case name first))
       (set)))

;; TODO: validate and only define actual custom element tags
(defn define-elements!
  "Define custom HTML elements covering all `tags`."
  [tags]
  (doseq [tag tags]
    (interop/define-element! tag)))

(defn shadow-ref
  "Get a :ref fn for a DOM element to render a given `comp` as its shadow root.
  The component should accept a single argument: the element's DOM reference."
  [comp]
  (fn [this]
    (when this
      (set! (.-shadow this) (.attachShadow this #js{:mode "open"}))
      (r/render [comp this] (.-shadow this)))))

(defn scope
  "Render `hiccup` inside a shadow DOM with the root element as the shadow host.
  Optionally takes scoped `css` to apply to the content in the shadow DOM."
  ([hiccup & [css]]
   (define-elements! (hiccup->custom-tags hiccup))
   (let [[[tag attr] children] (if (map? (second hiccup))
                                 (split-at 2 hiccup)
                                 (split-at 1 hiccup))
         comp (fn [_] (into [:<> (when css [:style css])] children))]
     [tag (assoc attr :ref (shadow-ref comp))])))
