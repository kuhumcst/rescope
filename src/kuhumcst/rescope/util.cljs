(ns kuhumcst.rescope.util
  (:require [clojure.string :as str]))

(defn prefixed
  "Add a `prefix` to an HTML `tag`, creating a valid custom HTML element name."
  [prefix tag]
  (let [reserved-tags #{"annotation-xml"
                        "color-profile"
                        "font-face"
                        "font-face-src"
                        "font-face-uri"
                        "font-face-format"
                        "font-face-name"
                        "missing-glyph"}
        prefixed-tag  (str prefix "-" tag)]
    (if (contains? reserved-tags prefixed-tag)
      (str prefixed-tag "-x")
      prefixed-tag)))

(defn data-*
  "Convert an XML attribute `k` into a data-* attribute."
  [k]
  (as-> k $
        (name $)
        (str/replace $ ":" "-")
        (str "data-" $)))
