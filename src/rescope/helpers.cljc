(ns rescope.helpers
  (:require [clojure.set :as set]
            [rescope.util :as util]))

(defn as-data-*
  [attr]
  (into {} (for [[k v] attr]
             [(keyword (util/data-* k)) v])))

(defn attr->data-attr
  "Convert all attributes into data-* attributes."
  [[tag attr & content :as node]]
  (if (map? attr)
    (assoc node 1 (as-data-* attr))
    node))

(defn rename-attr
  "Rename attr keys according to `kmap`."
  [kmap [tag attr & content :as node]]
  (if (and kmap (map? attr))
    (assoc node 1 (set/rename-keys attr (as-data-* kmap)))
    node))

(defn add-prefix
  "Transform a hiccup vector node `loc` to a valid custom element name by
  setting a custom `prefix`."
  [prefix node]
  (let [tag     (name (first node))
        new-tag (keyword (util/prefixed prefix tag))]
    (assoc node 0 new-tag)))

(defn meta-into-attr
  "Merge the element metadata into the attr. Mimics the behaviour of reagent."
  [[tag attr & content :as node]]
  (if-let [m (meta node)]
    (if (map? attr)
      (update node 1 merge m)
      (into [tag m] content))
    node))

(defn default-fn
  "Return function applying default transformations based on `opts`."
  [{:keys [attr-kmap prefix] :as opts}]
  (fn [node]
    (->> node
         (attr->data-attr)
         (rename-attr attr-kmap)
         (add-prefix prefix)
         (meta-into-attr))))
