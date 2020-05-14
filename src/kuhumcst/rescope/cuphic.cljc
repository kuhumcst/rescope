(ns kuhumcst.rescope.cuphic
  "Data transformations for hiccup."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [hickory.zip :as hzip]
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

;; Conforms to a superset of regular hiccup.
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

;; TODO: unnecessary? remove?
;; Check if a keyword Insertion is inside a map.
(def ^:private loc-in-map?
  (comp map? zip/node zip/up zip/up))

;; TODO: replace brittle deep-diff2, sometimes different diffs in clj vs. cljs
(defn- attr-bindings
  "Get the symbol->value mapping found when comparing `cattr` to `hattr`.
  Returns nil if the two attrs don't match."
  [cattr hattr]
  (let [diffs (dd/diff cattr hattr)]
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

            (recur (zip/next loc) ret)))))))

(defn- hicv
  "Helper function for normalised destructuring of a hiccup-vector `v`."
  [v]
  (if (map? (second v))
    v
    (into [(first v) {}] (rest v))))

(defn- bindings-delta
  "Get a delta of the local bindings as a map by comparing `cloc` to `hloc`.
  Will return nil if the two nodes do not match."
  [[cnode _ :as cloc] [hnode _ :as hloc]]
  (cond
    ;; Skip directly to the next node.
    (= cnode hnode)
    {}

    ;; Branches (vectors) are the real object of interest.
    (and (vector? cnode)
         (vector? hnode))
    (let [cv (hicv cnode)
          hv (hicv hnode)]
      (if (= (count cv)
             (count hv))
        ;; Return potential local bindings in tag and attr.
        (let [[ctag cattr] cv
              [htag hattr] hv]
          (merge
            (when (s/valid? ::var ctag)
              {ctag htag})
            (attr-bindings cattr hattr)))

        ;; TODO: handle variadic content here
        ;; Fail fast. Nil will bubble up and exit the loop.
        nil))

    ;; TODO: what about strings?
    ;; Leafs get skipped. They are handled as part of the content instead.
    (and (not (vector? cnode))
         (not (vector? cnode)))
    {}))

(defn- potential-match?
  "Helper function for optimising performance."
  [cuphic hiccup]
  ;; Account for the fact that the attr map is optional.
  (<= (dec (count hiccup))
      (count cuphic)
      (inc (count hiccup))))

(defn bindings
  "Get the symbol->value mapping found when comparing `cuphic` to `hiccup`.
  Returns nil if the hiccup does not match the cuphic.

  The two data structures are zipped through in parallel while their bindings
  are collected incrementally."
  [cuphic hiccup]
  (assert (s/valid? ::cuphic cuphic))                       ; elide in prod
  (when (potential-match? cuphic hiccup)
    (loop [cloc (hzip/hiccup-zip cuphic)
           hloc (hzip/hiccup-zip hiccup)
           ret  {}]
      (if (zip/end? cloc)                                   ; TODO: ...and hloc?
        ret
        (when-let [delta (bindings-delta cloc hloc)]
          (recur (zip/next cloc) (zip/next hloc) (merge ret delta)))))))

(defn matches
  "Returns the match, if any, of `hiccup` to `cuphic`."
  [cuphic hiccup]
  (when (bindings cuphic hiccup)
    hiccup))

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

  #_.)

