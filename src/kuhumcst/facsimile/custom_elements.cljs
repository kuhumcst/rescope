(ns kuhumcst.facsimile.custom-elements)

;; Useful documentation of the JS interop used in this code:
;; * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Details_of_the_Object_Model
;; * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/create
;; * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/defineProperty
;;
;; Explaining why super() can't be extended:
;; * https://stackoverflow.com/questions/43836886/failed-to-construct-customelement-error-when-javascript-file-is-placed-in-head

;; Note: js/Reflect.construct simply replicates a call to super() and in the
;; case of custom HTML elements, the constructor must otherwise be empty!
(defn- extend-class*
  [parent properties-obj]
  (let [child     (fn child* [] (js/Reflect.construct parent #js[] child*))
        prototype (js/Object.create (.-prototype parent) properties-obj)]
    (set! (.-prototype child) prototype)
    child))

(defn- js-props
  "Convert a map of properties into a properties object that can be consumed by
  extend-class*."
  [m]
  (let [wrap-method (fn [f]
                      (fn [& args]
                        (this-as this (f this args))))]
    (->> (for [[k v] m]
           [k {:value (if (fn? v)
                        (wrap-method v)
                        v)}])
         (into {})
         (clj->js))))

(defn extend-class
  "Create a subclass from the prototype of a `parent` class and a map of object
  `properties`."
  [parent properties]
  (extend-class* parent (js-props properties)))

(defn define
  "Define a custom element based on a tag name."
  [tag properties]
  (when (undefined? (js/window.customElements.get tag))
    (let [element (extend-class js/HTMLElement properties)]
      (js/window.customElements.define tag element))))
