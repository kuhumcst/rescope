(ns kuhumcst.rescope.cuphic
  "Data transformations for hiccup."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [lambdaisland.deep-diff2 :as dd]
            #?(:cljs [lambdaisland.deep-diff2.diff-impl :refer [Mismatch
                                                                Deletion
                                                                Insertion]]))
  #?(:clj (:import [lambdaisland.deep_diff2.diff_impl Mismatch
                                                      Deletion
                                                      Insertion])))

;; TODO: some way to handle variable length matches, e.g. using `...` symbol?


;; Every symbol prepended with ? has a value captured in its place.
(s/def ::var
  (s/and symbol?
         (comp #(str/starts-with? % "?") name)))            ; CLJS needs name

;; Everything prepended with _ is ignored.
(s/def ::ignored
  (s/and symbol?
         (comp #(str/starts-with? % "_") name)))            ; CLJS needs name

;; Possible insertion points for logic variables and other symbols.
;; Cannot be used in place of collections, e.g. hiccup vectors or attr maps!
(s/def ::slot
  (s/or
    :var ::var
    :ignored ::ignored
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
(defn same-shape?
  "Do the shapes of the `cuphic` and the `hiccup` resemble each other?"
  [cuphic hiccup]
  (let [empty-hiccup #(if (vector? %) % (empty %))]
    (when (= (count cuphic) (count hiccup))                 ; for performance
      (= (walk/postwalk empty-hiccup cuphic)
         (walk/postwalk empty-hiccup hiccup)))))

;; TODO: remove `loc-in-map?`? unnecessary with `resemble`
;; Check if a keyword Insertion is inside a map.
(def ^:private loc-in-map?
  (comp map? zip/node zip/up zip/up))

(defn bindings
  "Get the symbol->value mapping found when comparing `hiccup` to `cuphic`.
  Returns nil if the hiccup does not match the cuphic."
  [cuphic hiccup]
  (assert (s/valid? ::cuphic cuphic))                       ; elide in prod
  (when (same-shape? cuphic hiccup)
    (let [diffs (dd/diff cuphic hiccup)]
      (loop [loc (vector-map-zip diffs)
             ret {}]
        (if (zip/end? loc)
          ret
          (let [{:keys [+ -] :as node} (zip/node loc)]
            (condp instance? node
              Deletion
              nil

              ;; Insertions are problematic unless they are HTML attributes.
              Insertion
              (when (and (keyword? +)
                         (loc-in-map? loc))
                (recur (zip/next loc) ret))

              ;; Mismatches can indicate matching logic variables.
              Mismatch
              (when (symbol? -)
                (if (s/valid? ::var -)
                  (recur (zip/next loc) (assoc ret - +))
                  (recur (zip/next loc) ret)))

              (recur (zip/next loc) ret))))))))

(defn transform
  "Transform hiccup using cuphic from/to templates.

  Substitutes symbols in `to` with bound values from `hiccup` based on symbols
  in `from`. The cuphic templates can also be replaced with functions that
  either produce or consume a symbol->value map. "
  [from to hiccup]
  (when-let [symbol->value (if (fn? from)
                             (from hiccup)
                             (bindings from hiccup))]
    (if (fn? to)
      (to symbol->value)
      (walk/postwalk #(get symbol->value % %) to))))

(comment
  ;; Invalid example
  (bindings '[?tag {:style
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
  (bindings '[?tag {:style {:width ?width}}
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

  ;; Valid transformation using an fn as "to" template
  (transform '[?tag {:style {:width ?width}}
               [_ {} "p1"]
               [_ {} "p2"]]

             (fn [{:syms [?tag ?width]}]
               [:div
                [?tag {:style {:width ?width}}]
                [:p "width: " ?width]])

             [:span {:style {:width  "5px"
                             :height "10px"}}
              [:p {} "p1"]
              [:p {} "p2"]])

  ;; should be false
  (s/valid? ::cuphic '[?tag {:style {?df ?width}}
                       [:p {} "p1"]
                       [:p {} "p2"]])

  (s/conform ::cuphic '[?glen {:john _}])


  (same-shape? '[a b c] [1 2 3])
  (bindings '[?a _b c] [1 2 3])

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

