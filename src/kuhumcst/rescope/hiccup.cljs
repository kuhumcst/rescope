(ns kuhumcst.rescope.hiccup
  "Conform hiccup and inject reagent components into shadow roots."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.zip :as zip]
            [hickory.zip :as hzip]
            [kuhumcst.rescope.util :as util]
            [kuhumcst.rescope.cuphic :as cup]))

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
  (if (and kmap (map? attr))
    (zip/edit loc assoc 1 (set/rename-keys attr (as-data-* kmap)))
    loc))

;; Only modifies metadata. Later this is merged into attr by meta-into-attr.
(defn- inject
  "Insert transformed Hiccup when node `loc` matches one of the `transformers`.
  A `wrapper` fn taking [old-node new-node] as args can be supplied to modify
  the new node and its metadata. If no wrapper is supplied, the new node
  entirely replaces the old node in the tree."
  [wrapper transformers [[tag attr & content :as node] _ :as loc]]
  (if-let [hiccup (->> (map #(% node) transformers)
                       (remove nil?)
                       (first))]
    (let [new-node (if wrapper
                     (wrapper node hiccup)
                     (vary-meta hiccup assoc :replaced? true))]
      (zip/replace loc new-node))
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

(defn- skip-subtree
  "Skip the descendants of the current node."
  [[node _ :as loc]]
  (or (zip/right loc) (zip/rightmost loc)))

(defn- edit-branch
  [prefix attr-kmap wrapper transformers loc]
  (let [loc* (inject wrapper transformers loc)]
    (if (:replaced? (meta (zip/node loc*)))
      (skip-subtree loc*)
      (->> loc*
           (attr->data-attr)
           (rename-attr attr-kmap)
           (add-prefix prefix)
           (meta-into-attr)))))

(defn- ignore?
  "Return true if it makes sense to ignore this loc."
  [[[tag attr & content :as node] _ :as loc]]
  (and (vector? node)
       (= tag :<>)))                                        ; React fragments

(defn rewrite
  "Process relevant nodes of a zipper made from a `hiccup` tree based on `opts`.
  Return the transformed structure."
  ([hiccup {:keys [prefix attr-kmap wrapper transformers]
            :or   {prefix "rescope"}
            :as   opts}]
   ;; The way hiccup zips, every branch is a hiccup vector, while everything
   ;; else is a leaf. Leafs are usually strings, but can be other types too.
   (let [edit-node (fn [[node _ :as loc]]
                     ;; TODO: remove whitespace before zipping or at least handle leafs first
                     ;; currently whitespace is interfering with pattern matching!
                     (if (vector? node)
                       (edit-branch prefix attr-kmap wrapper transformers loc)
                       (edit-leaf loc)))]
     (loop [loc (hzip/hiccup-zip hiccup)]
       (if (zip/end? loc)
         (zip/root loc)
         (recur (zip/next (if (ignore? loc)
                            loc
                            (edit-node loc))))))))
  ([hiccup]
   (rewrite hiccup nil)))

(defn transformer
  "Make a transform fn to transform hiccup using cuphic from/to templates."
  [{:keys [from to]}]
  (partial cup/transform from to))
