(ns rescope.formats.xml
  "Parse XML as hiccup."
  (:require [clojure.string :as str]
            [hickory.core :as hickory]
            [hickory.utils :as hutils]))

;; Hickory in fact calls the same DOM method in hickory.core/parse, but has
;; been hardcoded to use the "text/html" mimetype rather than "xml"!
(defn- dom-parse
  [xml]
  (-> (js/DOMParser.)
      (.parseFromString xml "text/xml")
      (.-firstChild)))

;; Custom variation of Hickory's HiccupRepresentable (for XML):
;;  * Doesn't preserve comments.
;;  * Doesn't preserve empty whitespace.
;;  * Doesn't check for unescapable content (<style> and <script>).
(extend-protocol hickory/HiccupRepresentable
  object
  (as-hiccup [this]
    (condp = (.-nodeType this)
      hickory/Attribute [(hutils/lower-case-keyword (.-name this))
                         (.-value this)]
      hickory/Comment nil
      hickory/Document (map hickory/as-hiccup (.-childNodes this))
      hickory/DocumentType (hickory/format-doctype this)
      hickory/Element (let [tag      (-> (.-tagName this)
                                         (hutils/lower-case-keyword))
                            attr     (into {} (->> (.-attributes this)
                                                   (map hickory/as-hiccup)))
                            children (.-childNodes this)
                            content  (map hickory/as-hiccup children)]
                        (into [] (concat [tag attr] (remove nil? content))))
      hickory/Text (let [s (.-wholeText this)]
                     (when-not (str/blank? s)
                       (hutils/html-escape s))))))

(defn parse
  "Naïvely convert an `xml` string into a raw hiccup representation."
  [xml]
  (hickory/as-hiccup (dom-parse xml)))
