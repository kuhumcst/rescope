(ns kuhumcst.facsimile.core
  (:require [clojure.string :as str]
            [kuhumcst.facsimile.select :as select]
            [kuhumcst.facsimile.interop :as interop]
            [kuhumcst.facsimile.shadow :as shadow]))

(defn define-tags!
  "Define custom HTML elements covering all tags in the `hiccup`."
  [hiccup]
  (doseq [tag (->> (select/all hiccup)
                   (map (comp str/lower-case name first))
                   (set))]
    (interop/define! tag)))

(defn custom-html
  "Define all custom HTML elements of a `hiccup` tree and render its content in
   a shadow DOM together with scoped `css`."
  [hiccup css]
  (define-tags! hiccup)
  [shadow/scoped hiccup css])
