(ns kuhumcst.rescope.hiccup
  "Conform hiccup and inject reagent components into shadow roots."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.zip :as zip]
            [hickory.zip :as hzip]
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
(defn- inject
  "Insert shadow roots with components based on matches from the `injectors`."
  [injectors [[tag attr & content :as node] _ :as loc]]
  ;; TODO: unsure if lazyness is preserved - should it be a transducer?
  (if-let [comp (->> injectors
                     (map (fn [injector] (injector node)))
                     (remove nil?)
                     (first))]
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
    loc))

;; NOTE: all edits *must* preserve node metadata!
(defn- edit-branch
  [prefix attr-kmap injectors loc]
  (->> loc
       (inject injectors)
       (attr->data-attr)
       (rename-attr attr-kmap)                              ; TODO: remove?
       (add-prefix prefix)
       (meta-into-attr)))

(defn- ignore?
  "Return true if it makes sense to ignore this loc."
  [[[tag attr & content :as node] _ :as loc]]
  (and (vector? node)
       (= tag :<>)))                                        ; React fragments

(defn postprocess
  "Process relevant nodes of a zipper made from a `hiccup` tree based on `opts`.
  Return the transformed structure as valid HTML with shadow DOM injections.

  The hiccup structure is trimmed and the `prefix` is applied to all element
  tags in the tree. Attributes are renamed according to `attr-kmap` or converted
  into the data-* format. Finally, shadow roots are inserted based on the
  `injectors`, the HTML now being rendered by replacement components."
  ([hiccup {:keys [prefix attr-kmap injectors]
            :or   {prefix    "rescope"
                   attr-kmap {}
                   injectors []}
            :as   opts}]
   ;; The way hiccup zips, every branch is a hiccup vector, while everything
   ;; else is a leaf. Leafs are usually strings, but can be other types too.
   (let [edit-node (fn [[node _ :as loc]]
                     ;; TODO: remove whitespace before zipping or at least handle leafs first
                     ;; currently whitespace is interfering with pattern matching!
                     (if (vector? node)
                       (edit-branch prefix attr-kmap injectors loc)
                       (edit-leaf loc)))]
     (loop [loc (hzip/hiccup-zip hiccup)]
       (if (zip/end? loc)
         (zip/root loc)
         (recur (zip/next (if (ignore? loc)
                            loc
                            (edit-node loc))))))))
  ([hiccup]
   (postprocess hiccup nil)))
