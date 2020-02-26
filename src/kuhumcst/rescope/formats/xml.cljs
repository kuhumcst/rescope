(ns kuhumcst.rescope.formats.xml
  "Parse XML as hiccup and transform the parse tree."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [clojure.set :as set]
            [hickory.core :as hickory]
            [kuhumcst.rescope.util :as util]
            [kuhumcst.rescope.core :as rescope]))

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
    (zip/edit loc assoc-meta :ref (rescope/shadow-ref comp))
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

(defn- edit-leaf
  [[node _ :as loc]]
  (if (string? node)
    (->> loc
         (trim-str)
         (remove-comment))
    node))

(defn- edit-branch
  [prefix attr-kmap rewrite-fn loc]
  (->> loc
       (inject-shadow rewrite-fn)
       (attr->data-attr)
       (rename-attr attr-kmap)                              ; TODO: remove?
       (add-prefix prefix)
       (meta-into-attr)))

(defn postprocessor
  "Create an fn for postprocessing a XML-derived hiccup zipper loc.

  The hiccup structure is trimmed and the `prefix` is applied to all element
  tags in the tree. Attributes are renamed according to `attr-kmap` or converted
  into the data-* format. Finally, shadow roots are inserted based on the
  `rewrite-fn`, the HTML now being rendered by replacement components."
  [prefix attr-kmap rewrite-fn]
  (rescope/postprocessor edit-leaf
                         (partial edit-branch prefix attr-kmap rewrite-fn)))

;; Hickory in fact calls the same DOM method in hickory.core/parse, but has
;; been hardcoded to use the "text/html" mimetype rather than "xml"!
(defn- dom-parse
  [xml]
  (-> (js/DOMParser.)
      (.parseFromString xml "text/xml")
      (.-firstChild)))

(defn parse
  "Naïvely convert an `xml` string into an initial hiccup representation."
  [xml]
  (-> xml dom-parse hickory/as-hiccup))
