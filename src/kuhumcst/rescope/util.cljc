(ns kuhumcst.rescope.util
  (:require [clojure.string :as str]))

;; https://html.spec.whatwg.org/multipage/custom-elements.html#valid-custom-element-name
(def reserved-tags
  #{"annotation-xml"
    "color-profile"
    "font-face"
    "font-face-src"
    "font-face-uri"
    "font-face-format"
    "font-face-name"
    "missing-glyph"})

(def custom-tag
  #"\w+(-\w+)+")

(defn valid-custom-tag?
  [tag-str]
  (and (not (reserved-tags tag-str))
       (re-matches custom-tag tag-str)))

(defn prefixed
  "Add a `prefix` to an HTML `tag`, creating a valid custom HTML element name."
  [prefix tag]
  (let [prefixed-tag (str prefix "-" tag)]
    (if (reserved-tags prefixed-tag)
      (str prefixed-tag "-x")
      prefixed-tag)))

(defn data-*
  "Convert an XML attribute `k` into a data-* attribute."
  [k]
  (as-> k $
        (name $)
        (str/replace $ ":" "-")
        (str "data-" $)))
