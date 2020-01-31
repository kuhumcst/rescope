(ns kuhumcst.facsimile.xml
  "Parse XML as hiccup and select elements from a zipped representation."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]))

(defn- trim-str
  "Remove any blank and trim any non-blank strings of a string node `loc`."
  [[node _ :as loc]]
  (if (str/blank? node)
    (zip/remove loc)
    (zip/replace loc (str/trim node))))

(defn- trim-vec
  "Remove the empty attr from a hiccup vector node `loc`."
  [[node _ :as loc]]
  (if (and (map? (second node))
           (empty? (second node)))
    (zip/replace loc (vec (concat (subvec node 0 1)
                                  (subvec node 2))))
    loc))

;; https://stackoverflow.com/questions/22545621/do-custom-elements-require-a-dash-in-their-name#tab-top
(def illegal-names
  #{"annotation-xml"
    "color-profile"
    "font-face"
    "font-face-src"
    "font-face-uri"
    "font-face-format"
    "font-face-name"
    "missing-glyph"})

(defn- add-prefix
  "Transform a hiccup vector node `loc` to a valid custom element name by
  setting a custom `prefix`."
  [prefix [node _ :as loc]]
  (let [tag          (name (first node))
        prefixed-tag (str prefix "-" tag)
        new-tag      (keyword (if (contains? illegal-names prefixed-tag)
                                (str prefixed-tag "-x")
                                prefixed-tag))]
    (zip/replace loc (apply vector new-tag (rest node)))))

(defn- preprocess-fn
  "Create an fn for preprocessing an XML-derived hiccup zipper `loc` based on a
  `root` loc. The initial hiccup structure is trimmed and the name of the root
  element is used as a prefix for all elements."
  [[root-node _ :as root]]
  (fn [[node _ :as loc]]
    (let [root-element (name (first root-node))]
      (cond
        (string? node) (trim-str loc)
        (vector? node) (->> (trim-vec loc)
                            (add-prefix root-element))))))

(defn element
  "Create an element selector predicate based on a `tag` name."
  ([tag]
   (every-pred vector? (comp (partial = tag) first)))
  ([]
   vector?))

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
  "Select all nodes satisfying `preds` from zipper starting at `root` loc."
  [root & preds]
  (loop [[node _ :as loc] root
         nodes []]
    (if (zip/end? loc)
      nodes
      (recur (zip/next loc) (if ((apply every-pred preds) node)
                              (conj nodes node)
                              nodes)))))

;; TODO: optimise - quit loop faster?
(defn select
  "Select the first node satisfying `preds` from zipper starting at `root` loc."
  [root & preds]
  (first (apply select-all root preds)))

(defn transform
  "Apply `transform-fn` to every loc in zipper starting at `root` loc and return
  the transformed structure."
  [transform-fn root]
  (loop [loc root]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (transform-fn loc))))))

(def zip
  hzip/hiccup-zip)

;; Note: Hickory in fact calls the same DOM method in `hickory.core/parse`, but
;; uses the "text/html" mimetype rather than "text/xml" like we need.
(defn- dom-parse
  [xml]
  (-> (js/DOMParser.)
      (.parseFromString xml "text/xml")
      (.-firstChild)))

;; TODO: handle XML comments?
(defn parse
  "Convert an `xml` string into a hiccup representation."
  [xml]
  (let [root (-> xml dom-parse hickory/as-hiccup zip)]
    (transform (preprocess-fn root) root)))
