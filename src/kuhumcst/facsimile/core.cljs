(ns kuhumcst.facsimile.core
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [hickory.core :as h]
            [hickory.zip :as hz]))

;; Note: Hickory in fact calls the same DOM method in `hickory.core/parse`, but
;; uses the "text/html" mimetype rather than "text/xml" like we need.
(defn- parse-xml
  [xml]
  (.-firstChild (.parseFromString (js/DOMParser.) xml "text/xml")))

(defn trim-str
  "Remove any blank and trim any non-blank strings of a string node `loc`."
  [[node _ :as loc]]
  (if (str/blank? node)
    (zip/remove loc)
    (zip/replace loc (str/trim node))))

(defn trim-vec
  "Remove the empty attr from a hiccup vector node `loc`."
  [[node _ :as loc]]
  (if (and (map? (second node))
           (empty? (second node)))
    (zip/replace loc (vec (concat (subvec node 0 1)
                                  (subvec node 2))))
    loc))

(defn prefixed
  "Transform a hiccup vector node `loc` to a valid custom element name by
  setting a custom `prefix`."
  [prefix [node _ :as loc]]
  (let [element (name (first node))]
    (zip/replace loc (vec (concat [(keyword (str prefix "-" element))]
                                  (rest node))))))

(defn clean-xml-hiccup
  "Clean an XML-derived hiccup zipper `loc` based on a `root` loc. The name of
  the root element is used as a prefix for all elements."
  [[root-node _ :as root] [node _ :as loc]]
  (let [root-element (name (first root-node))]
    (cond
      (string? node) (trim-str loc)
      (vector? node) (->> (trim-vec loc)
                          (prefixed root-element)))))

(defn transform-zipper
  "Apply `transform-fn` to every loc in a zipper starting at the `root` loc,
  returning the transformed structure."
  [transform-fn root]
  (loop [loc root]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (transform-fn loc))))))

(defn xml->hiccup
  "Convert an `xml` string into a hiccup representation."
  [xml]
  (let [root (-> (parse-xml xml)
                 (h/as-hiccup)
                 (hz/hiccup-zip))]
    (transform-zipper (partial clean-xml-hiccup root) root)))
