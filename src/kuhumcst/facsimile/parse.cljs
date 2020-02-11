(ns kuhumcst.facsimile.parse
  "Parse XML as hiccup and select elements from the parsed representation."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]
            [kuhumcst.facsimile.util :as util]))

(defn- trim-str
  "Remove any blank strings from a string node `loc`."
  [[node _ :as loc]]
  (if (str/blank? node)
    (zip/remove loc)
    loc))

(defn- remove-comment
  "Remove any strings converted from XML comments from a string node `loc`."
  [[node _ :as loc]]
  (if (and (str/starts-with? node "<!--")
           (str/ends-with? node "-->"))
    (zip/remove loc)
    loc))

(defn- trim-vec
  "Remove the empty attr from a hiccup vector node `loc`."
  [[[tag attr & content :as node] _ :as loc]]
  (if (and (map? attr)
           (empty? attr))
    (zip/replace loc (into [tag] content))
    loc))

(defn- convert-attr
  "Convert all XML attributes into data-* attributes."
  [[[tag attr & content :as node] _ :as loc]]
  (if (map? attr)
    (zip/replace loc (assoc node 1 (into {} (for [[k v] attr]
                                              [(keyword (util/data-* k)) v]))))
    loc))

(defn- add-prefix
  "Transform a hiccup vector node `loc` to a valid custom element name by
  setting a custom `prefix`."
  [prefix [node _ :as loc]]
  (let [tag     (name (first node))
        new-tag (keyword (util/prefixed prefix tag))]
    (zip/replace loc (apply vector new-tag (rest node)))))

(defn- preprocess-fn
  "Create an fn for preprocessing an XML-derived hiccup zipper `loc` based on a
  `prefix-tag`. The hiccup structure is trimmed and the prefix is applied to all
  elements."
  [prefix-tag]
  (fn [[node _ :as loc]]
    (cond
      (string? node) (->> (trim-str loc)
                          (remove-comment))
      (vector? node) (->> (trim-vec loc)
                          (convert-attr)
                          (add-prefix (name prefix-tag))))))

(defn element
  "Create an element selector predicate for element `tags`. Will select elements
  present in the list of tags. Selects all elements if no tags are specified."
  [& tags]
  (if (empty? tags)
    vector?
    (every-pred vector? (comp (set tags) first))))

(defn attr
  "Create an attribute selector predicate based on `attr`. Passing a set as attr
  will test for the existence of attribute keys, while passing a map will test
  for matching key-value pairs of attributes."
  [attr]
  (let [contains-attr? (fn [[_ m]]
                         (cond
                           (set? attr) (every? (partial contains? m) attr)
                           (map? attr) (every? (set m) attr)
                           :else (contains? m attr)))]
    (every-pred vector? (comp map? second) contains-attr?)))

(defn select-all
  "Select all elements satisfying `preds` from an `hiccup` tree. If no
  predicates are specified, all elements in the hiccup will be returned."
  [hiccup & preds]
  (let [satisfies-preds? (if (empty? preds)
                           vector?
                           (apply every-pred preds))]
    (loop [[node _ :as loc] (hzip/hiccup-zip hiccup)
           nodes []]
      (if (zip/end? loc)
        nodes
        (recur (zip/next loc) (if (satisfies-preds? node)
                                (conj nodes node)
                                nodes))))))

;; TODO: optimise - quit loop faster?
(defn select
  "Select the first element satisfying `preds` from an `hiccup` tree."
  [hiccup & preds]
  (first (apply select-all hiccup preds)))

(defn transform
  "Apply `transform-fn` to every loc in the zipper starting at the root of an
  `hiccup` tree and return the transformed structure."
  [transform-fn hiccup]
  (loop [loc (hzip/hiccup-zip hiccup)]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (transform-fn loc))))))

;; Hickory in fact calls the same DOM method in `hickory.core/parse`, but has
;; been hardcoded to use the "text/html" mimetype rather than "text/xml"!
(defn- dom-parse
  [xml]
  (-> (js/DOMParser.)
      (.parseFromString xml "text/xml")
      (.-firstChild)))

(defn xml->hiccup
  "Convert an `xml` string into a hiccup representation."
  [xml]
  (-> xml dom-parse hickory/as-hiccup))

(defn preprocess
  [hiccup]
  (transform (preprocess-fn (first hiccup)) hiccup))