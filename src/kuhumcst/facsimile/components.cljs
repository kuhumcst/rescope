(ns kuhumcst.facsimile.components
  (:require [kuhumcst.facsimile.xml :as xml]
            [kuhumcst.facsimile.custom-elements :as custom-elements]))

(def example-properties
  {:connectedCallback        (fn [this]
                               (set! (.-color (.-style this)) "green")
                               (set! (.-display (.-style this)) "block"))
   :disconnectedCallback     (fn [this] #_(js/console.log "disconnectedCallback" this))
   :attributeChangedCallback (fn [this] (js/console.log "attributeChangedCallback" this))})

(defn xml-view
  [xml]
  (let [tags (->> (xml/select-all xml)
                  (map (comp name first))
                  (set))]
    (doseq [tag tags]
      (custom-elements/define tag example-properties))
    xml))
