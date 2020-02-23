(ns kuhumcst.facsimile.parse
  "Parse XML as hiccup and transform the parse tree."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [clojure.set :as set]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]
            [kuhumcst.facsimile.util :as util]
            [kuhumcst.facsimile.shadow :as shadow]))

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

(defn- as-data-*
  [attr]
  (into {} (for [[k v] attr]
             [(keyword (util/data-* k)) v])))

(defn- attr->data-attr
  "Convert all attributes into data-* attributes."
  [[[tag attr & content :as node] _ :as loc]]
  (if (map? attr)
    (zip/edit loc assoc 1 (as-data-* attr))
    loc))

(defn- rename-attr
  "Rename attr keys according to `kmap`."
  [kmap [[tag attr & content :as node] _ :as loc]]
  (if (map? attr)
    (zip/edit loc assoc 1 (set/rename-keys attr (as-data-* kmap)))
    loc))

(defn- assoc-meta
  [o k v]
  (with-meta o (assoc (meta o) k v)))

;; Only modifies metadata. Later this is merged into attr by meta-into-attr.
(defn- inject-shadow
  "Insert shadow roots with components based on matches from `rewrite-fn`."
  [rewrite-fn [[tag attr & content :as node] _ :as loc]]
  (if-let [comp (rewrite-fn node)]
    (zip/edit loc assoc-meta :ref (shadow/root-ref comp))
    loc))

(defn- add-prefix
  "Transform a hiccup vector node `loc` to a valid custom element name by
  setting a custom `prefix`."
  [prefix [node _ :as loc]]
  (let [tag     (name (first node))
        new-tag (keyword (util/prefixed prefix tag))]
    (zip/edit loc assoc 0 new-tag)))

(defn- meta-into-attr
  "Merge the element metadata into the attr. Mimics the behaviour of reagent."
  [[[tag attr & content :as node] _ :as loc]]
  (if-let [m (meta node)]
    (zip/edit loc update 1 merge m)
    loc))

(defn patch-fn
  "Create an fn for processing an XML-derived hiccup zipper `loc` based on a
  `prefix`, an `attr-kmap`, and a `rewrite-fn`.

  The hiccup structure is trimmed and the prefix is applied to all element tags
  in the tree. Attributes are renamed according to attr-kmap or converted into
  the data-* format. Finally, shadow roots are inserted based on the rewrite-fn,
  the HTML now being rendered by any potential replacement components."
  [prefix attr-kmap rewrite-fn]
  (fn [[node _ :as loc]]
    (cond
      (string? node) (->> loc
                          (trim-str)
                          (remove-comment))
      ;; Note: all operations must edit the existing node to preserve metadata!
      (vector? node) (->> loc
                          (inject-shadow rewrite-fn)
                          (attr->data-attr)
                          (rename-attr attr-kmap)           ; TODO: remove?
                          (add-prefix prefix)
                          (meta-into-attr)))))

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
  "Naïvely convert an `xml` string into an initial hiccup representation."
  [xml]
  (-> xml dom-parse hickory/as-hiccup))
