(ns kuhumcst.rescope.formats.xml
  "Parse XML as hiccup."
  (:require [hickory.core :as hickory]))

;; Hickory in fact calls the same DOM method in hickory.core/parse, but has
;; been hardcoded to use the "text/html" mimetype rather than "xml"!
(defn- dom-parse
  [xml]
  (-> (js/DOMParser.)
      (.parseFromString xml "text/xml")
      (.-firstChild)))

(defn parse
  "Naïvely convert an `xml` string into a raw hiccup representation."
  [xml]
  (-> xml dom-parse hickory/as-hiccup))
