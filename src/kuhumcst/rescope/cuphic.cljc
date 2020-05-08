(ns kuhumcst.rescope.cuphic
  "Data transformations for hiccup."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [lambdaisland.deep-diff2 :as dd]
            #?(:clj  [lambdaisland.deep-diff2.diff-impl]
               :cljs [lambdaisland.deep-diff2.diff-impl :refer [Mismatch
                                                                Deletion
                                                                Insertion]]))
  ;; Explaining the conversion from dashes to underscores:
  ;; https://stackoverflow.com/questions/4580462/cant-import-clojure-records
  #?(:clj
     (:import [lambdaisland.deep_diff2.diff_impl Mismatch
                                                 Deletion
                                                 Insertion])))

;; TODO: some way to handle variable length matches, e.g. using `...` symbol?


(s/def ::logic-variable
  (s/and symbol?
         (comp #(str/starts-with? % "?") name)))

;; A possible insertion point for logic variables (represented by symbols).
;; Cannot be used in place of collections, e.g. hiccup vectors or attr maps!
(s/def ::slot
  (s/or
    :var ::logic-variable
    :other (complement coll?)))

(s/def ::attr
  (s/map-of keyword? (s/or
                       :slot ::slot
                       :map ::attr)))

(s/def ::cuphic
  (s/and vector?
         (s/conformer vec vec)                              ; unform as vector
         (s/cat
           :tag ::slot
           :attr (s/? ::attr)
           :content (s/* (s/or
                           :cuphic ::cuphic
                           :other (complement map?))))))

(defn vector-map-zip
  "Also zips maps in addition to zipping vectors."
  [root]
  (zip/zipper (some-fn vector? (every-pred map? (complement record?)))
              seq
              (fn [node children] (with-meta (vec children) (meta node)))
              root))

;; The diffing algorithm doesn't work well for vectors of dissimilar shape,
;; turning mismatches into pairs of Deletions and Insertions instead.
(defn resemble
  "Check that the shapes of `cuphic` and `hiccup` resemble each other."
  [cuphic hiccup]
  (let [coerce-shape #(if (vector? %) % (empty %))]
    (= (walk/postwalk coerce-shape cuphic)
       (walk/postwalk coerce-shape hiccup))))

(defn logic-vars
  "Get the symbol->value mapping found when comparing `hiccup` to `cuphic`.
  Returns nil if the hiccup does not match the cuphic.

  Relies on the data structure created by deep-diff2. The presence of a Mismatch
  record indicates a matching logic variable if the replaced value is a symbol.
  Deletions are always problematic; as are Insertions, unless they happen to be
  new HTML attributes."
  [cuphic hiccup]
  (assert (s/valid? ::cuphic cuphic))                       ; elide in prod
  (when (resemble cuphic hiccup)
    (let [diffs (dd/diff cuphic hiccup)]
      (loop [loc      (vector-map-zip diffs)
             bindings {}]
        (let [node (zip/node loc)]
          (when-not (instance? Deletion node)               ; fail fast
            (cond
              (zip/end? loc) bindings

              ;; TODO: what about randomly inserted keywords not in a map?
              (instance? Insertion node) (when (keyword? (:+ node))
                                           (recur (zip/next loc) bindings))

              (instance? Mismatch node) (when (symbol? (:- node))
                                          (recur (zip/next loc) (assoc bindings
                                                                  (:- node)
                                                                  (:+ node))))
              :else (recur (zip/next loc) bindings))))))))

(defn transform
  "Transform hiccup using cuphic from/to templates.

  Substitutes logic variables in `to` with values found in `hiccup` based on
  logic variables in `from`."
  [from to hiccup]
  (when-let [symbol->value (logic-vars from hiccup)]
    (walk/postwalk #(or (symbol->value %) %) to)))

(defn transformer
  "Make a transform fn to transform hiccup using cuphic from/to templates."
  [{:keys [from to]}]
  (partial transform from to))

(comment
  ;; Invalid example
  (logic-vars '[?tag {:style
                      ;; should fail here
                      {?df ?width}}
                [:p {} "p1"]
                [:p {} "p2"]]
              [:div {:style {:width  "5px"
                             :height "10px"}}
               [:p {} "p1"
                ;; should fail here, but will not reach due to spec assert
                [:glen]]
               [:p {} "p2"]])

  ;; Valid logic var extraction example
  (logic-vars '[?tag {:style {:width ?width}}
                [:p {} "p1"]
                [:p {} "p2"]]
              [:div {:style {:width  "5px"
                             :height "10px"}}
               [:p {} "p1"]
               [:p {} "p2"]])

  ;; Valid transformation example
  (transform '[?tag {:style {:width ?width}}
               [_ {} "p1"]
               [_ {} "p2"]]

             '[:div
               [?tag {:style {:width ?width}}]
               [:p "width: " ?width]]

             [:span {:style {:width  "5px"
                             :height "10px"}}
              [:p {} "p1"]
              [:p {} "p2"]])

  ;; should be false
  (s/valid? ::cuphic '[?tag {:style {?df ?width}}
                       [:p {} "p1"]
                       [:p {} "p2"]])


  (resemble '[a b c] [1 2 3])
  (logic-vars '[a b c] [1 2 3])

  ;; Walk/zip through both structures, replacing every vector with a new vector
  ;; where every non-vector item is `(empty x)` and then just check equality.
  (walk/postwalk-demo [[1 2 3] 4 5])
  (walk/postwalk #(if (vector? %) % (empty %))
                 '[:div {:style "class"
                         :id    "id"}
                   [:p {:on-click do-stuff}
                    "text"]
                   [:p "more text" 1 2 3]
                   [:p "text"
                    [:span "x"]
                    [:em "y"]]])


  #_.)

