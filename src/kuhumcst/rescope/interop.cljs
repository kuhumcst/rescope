(ns kuhumcst.rescope.interop)

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
  "Private implementation of extend-class, expecting only JavaScript types. This
  is simply a way to emulate the more modern class-based JS while still using
  its prototype-based subclassing.

  The primary goal was to emulate a call to super() in the constructor. This is
  a requirement when creating custom HTML components."
  [parent props-obj construct]
  (let [child     (fn child* []
                    (let [obj (js/Reflect.construct parent #js[] child*)]
                      (when construct
                        (construct obj))
                      obj))
        prototype (js/Object.create (.-prototype parent) (or props-obj
                                                             js/undefined))]
    (set! (.-prototype child) prototype)
    child))

(defn- js-props
  "Convert a map of properties into a properties object that can be consumed by
  extend-class*."
  [props]
  (let [wrap-method (fn [f]
                      (fn [& args]
                        (this-as this (f this args))))]
    (some->> (for [[k v] props]
               [k {:value (if (fn? v)
                            (wrap-method v)
                            v)}])
             (into {})
             (clj->js))))

(defn extend-class
  "Extend the prototype of a `parent` class with a map of object `props`."
  [parent & [{:keys [construct] :as props}]]
  (extend-class* parent (js-props (dissoc props :construct)) construct))

(defn define-element!
  "Define a custom element based on a `tag` name. Can optionally take a map of
  `props` to refine the prototype of the new HTMLElement subclass."
  [tag & [props]]
  (when (undefined? (js/window.customElements.get tag))
    (let [element (extend-class js/HTMLElement props)]
      (js/window.customElements.define tag element))))
