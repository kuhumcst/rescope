(ns kuhumcst.facsimile.util)

(defn prefixed
  "Prefix an HTML element tag, creating a valid custom HTML element name."
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
