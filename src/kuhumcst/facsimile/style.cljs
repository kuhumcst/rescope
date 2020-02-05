(ns kuhumcst.facsimile.style
  (:require [clojure.string :as str]
            [kuhumcst.facsimile.util :as util]))

(defn prefix-css
  "Super hairy, write-only code for adding a `prefix` to all element selectors
  in a string of `css` code."
  [prefix css]
  (let [css-comment      #"/\*.*\*/"
        css-rule         #"([^{]+)(\s*\{[^}]*\}\s*)"
        selector-split   #"([\s\(\)]+)|([^\s\(\)]+)"
        element-selector #"([a-zA-Z-]+).*"
        add-prefix       (fn [?element]
                           (if (and (string? ?element)
                                    (re-matches element-selector ?element))
                             (util/prefixed prefix ?element)
                             ?element))]
    (as-> css $
          (str/replace $ css-comment "")
          (re-seq css-rule $)
          (for [[_ selector declaration] $]
            [(for [?elements (map rest (re-seq selector-split selector))]
               (map add-prefix ?elements))
             declaration])
          (flatten $)
          (apply str $))))

;; TODO: remove?
(defn prefix-garden-css
  "Does the same as prefix-css, but directly on a vector of garden CSS rules."
  [prefix garden-css]
  (let [tag?       #(and (or (keyword? %)
                             (string? %)
                             (symbol? %))
                         (re-matches #"^[a-zA-Z-].*" (name %)))
        add-prefix (fn [x]
                     (if (tag? x)
                       (util/prefixed prefix (name x))
                       x))]
    (mapv (partial mapv add-prefix) garden-css)))
