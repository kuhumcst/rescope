(ns kuhumcst.rescope.cuphic
  "Data transformations for hiccup."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
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

;; TODO: mk-injector?
;; TODO: some way to handle variable length matches, e.g. using `...` symbol?


;; An insertion point for a logic variable, represented by symbols in cuphic.
;; Cannot be used in place of collections, e.g. hiccup vectors or attr maps!
(s/def ::?var
  (s/or
    :symbol symbol?
    :other (complement coll?)))

(s/def ::attr
  (s/map-of keyword? (s/or
                       :?var ::?var
                       :map ::attr)))

;; https://stackoverflow.com/questions/39147258/clojure-spec-unform-returning-non-conforming-values
(s/def ::cuphic
  (s/and vector?
         (s/conformer vec vec)                              ; unform as vector
         (s/cat
           :tag ::?var
           :attr (s/? ::attr)
           :content (s/* ::cuphic-content))))

(s/def ::cuphic-content
  (s/or
    :cuphic ::cuphic
    :other (complement map?)))

(defn vector-map-zip
  "In addition to zipping through vectors, also zips maps."
  [root]
  (zip/zipper (some-fn vector? (every-pred map? (complement record?)))
              seq
              (fn [node children] (with-meta (vec children) (meta node)))
              root))

;; TODO: rewrite and put in docstring?
;; First, ensure that only valid cuphic is supplied, i.e. no symbols in wrong
;; places?
;; Then, the basic algorithm
;; * zip through data structure
;; * look for vector of deep diff records
;; * ignore Insertion? for maps (attr)
;;    - ... or to put it another way, only allow Insertion of keywords, not vectors or other types?
;; * abort on Deletion
;; * abort on Mismatches where :- is not a symbol
;; * collect Mismatches where :- is a symbol
;; * make it into a mapping variable->value

(defn logic-vars
  "Get the symbol->value mapping found when comparing `hiccup` to `cuphic`.
  Returns nil if the hiccup does not match the cuphic."
  [cuphic hiccup]
  ;; TODO: make this check recursively... somehow? assumption breaks otherwise
  ;; The diffing algorithm doesn't work well for vectors of dissimilar length,
  ;; turning mismatches into pairs of Deletions and Insertions instead.
  (when (and (= (count cuphic)
                (count hiccup)))

    ;; Assert that logic variables are placed in compatible places.
    ;; This assertion can be elided in production environments, assuming that
    ;; the provided cuphic has already been tested during development.
    (assert (s/valid? ::cuphic cuphic))

    ;; Zipping through the data structure returned by the diff, every valid
    ;; Mismatch is collected in a map. The loop will only recur as long as it
    ;; doesn't run into any clear incompatibilities.
    (let [diffs (dd/diff cuphic hiccup)]
      (loop [loc        (vector-map-zip diffs)
             mismatches {}]
        (let [node (zip/node loc)]
          (when-not (instance? Deletion node)               ;; abort early
            (cond
              (zip/end? loc) mismatches

              ;; TODO: what about randomly inserted keywords not in a map?
              ;; Ignore insertions of keywords. Abort otherwise.
              (instance? Insertion node) (when (keyword? (:+ node))
                                           (recur (zip/next loc)
                                                  mismatches))

              ;; Only logical variable "mismatches" are allowed.
              ;; Any other mismatches should not be allowed, e.g. :p -> :a.
              (instance? Mismatch node) (when (symbol? (:- node))
                                          (recur (zip/next loc)
                                                 (assoc mismatches (:- node)
                                                                   (:+ node))))

              :else (recur (zip/next loc)
                           mismatches))))))))

(defn transform
  "Transform hiccup using cuphic from/to templates.

  Substitutes logic variables in `to` with values found in `hiccup` based on
  logic variables in `from`."
  [from to hiccup]
  (let [symbol->value (logic-vars from hiccup)]
    (walk/postwalk (fn [x]
                     (if-let [replacement (symbol->value x)]
                       replacement
                       x))
                   to)))

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

  (keyword? ?sdsd)
  #_.)

