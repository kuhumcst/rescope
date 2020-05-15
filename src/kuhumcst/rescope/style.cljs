(ns kuhumcst.rescope.style
  (:require [clojure.string :as str]
            [cuphic.util :as util]))

(defn remove-comments
  "Remove comments from a piece of `css`."
  [css]
  (str/replace css #"/\*.*\*/" ""))

;; TODO: support @import, @media, etc. special rules
(defn prefix-element-selectors
  "Super hairy, write-only code for adding a `prefix` to all element selectors
  in a piece of `css`."
  [prefix css]
  (let [css-rule         #"([^{]+)(\s*\{[^}]*\}\s*)"
        ?element-split   #"([\s\(\)]+)|([^\s\(\)]+)"
        element-selector #"([a-zA-Z-]+).*"
        add-prefix       (fn [?element]
                           (if (and (string? ?element)
                                    (re-matches element-selector ?element))
                             (util/prefixed prefix ?element)
                             ?element))]
    (as-> css $
          (re-seq css-rule $)
          (for [[_ selector declaration] $]
            [(for [?elements (map rest (re-seq ?element-split selector))]
               (map add-prefix ?elements))
             declaration])
          (flatten $)
          (apply str $))))

;; TODO: support attr(...) CSS function
;; https://developer.mozilla.org/en-US/docs/Web/CSS/attr
;; https://css-tricks.com/css-attr-function-got-nothin-custom-properties/
(defn convert-to-data-*
  "More Perl-wannabe, garbage code for prefixing data-* to attribute selectors
  in a piece of `css`."
  [css]
  (let [css-rule      #"([^{]+)(\s*\{[^}]*\}\s*)"
        ?attr-split   #"(\[[^\]]+\])|([^\[]+)"
        attr-selector #"\[([a-zA-Z]+)(.*)"
        add-data-*    (fn [?attr]
                        (if-let [[_ k x] (and (string? ?attr)
                                              (re-matches attr-selector ?attr))]
                          (str "[" (util/data-* k) x)
                          ?attr))]
    (as-> css $
          (re-seq css-rule $)
          (for [[_ selector declaration] $]
            [(for [?attrs (map rest (re-seq ?attr-split selector))]
               (map add-data-* ?attrs))
             declaration])
          (flatten $)
          (apply str $))))

(defn trim-blank-space
  "Remove superfluous newlines."
  [css]
  (-> (str/replace css #" +\n" "\n")                        ; line-end spaces
      (str/replace #"\n\n+" "\n\n")                         ; multiple newlines
      (str/triml)))

(defn prefix-css
  "Patch a piece of `css` written for the original document structure so that it
  matches the postprocessed hiccup. Will remove comments, add `prefix` to
  element selectors, and convert all attribute selectors to the data-* style."
  [prefix css]
  (->> (remove-comments css)
       (prefix-element-selectors prefix)
       (convert-to-data-*)
       (trim-blank-space)))
