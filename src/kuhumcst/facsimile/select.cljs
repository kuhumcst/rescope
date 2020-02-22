(ns kuhumcst.facsimile.select
  "Select elements in a hiccup tree."
  (:require [clojure.zip :as zip]
            [hickory.zip :as hzip]))

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

(defn all
  "Select all elements satisfying `preds` in a `hiccup` tree. If no predicates
  are specified, all elements in the hiccup will be returned."
  [hiccup & preds]
  (let [matches? (apply every-pred (or preds [vector?]))]
    (loop [[node _ :as loc] (hzip/hiccup-zip hiccup)
           nodes []]
      (if (zip/end? loc)
        nodes
        (recur (zip/next loc) (if (matches? node)
                                (conj nodes node)
                                nodes))))))

;; TODO: optimise - quit loop faster?
(defn one
  "Select the first element satisfying `preds` in a `hiccup` tree."
  [hiccup & preds]
  (first (apply all hiccup preds)))
