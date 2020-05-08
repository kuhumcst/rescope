(ns user
  (:require [clojure.pprint :refer [pprint]]
            [shadow.resource :as resource]
            [reagent.dom :as rdom]
            [meander.epsilon :as m]
            [kuhumcst.rescope.formats.xml :as xml]
            [kuhumcst.rescope.hiccup :as hic]
            [kuhumcst.rescope.select :as select]
            [kuhumcst.rescope.style :as style]
            [kuhumcst.rescope.interop :as interop]
            [kuhumcst.rescope.core :as rescope]))

;(interop/request {:url         "https://fonts.googleapis.com/css?family=Noto+Sans+HK&display=swap"
;                  :on-progress (fn [e]
;                                 (println (interop/event-data e)))})

(def tei-example
  ;(resource/inline "examples/tei/1151anno-anno-tei.xml"))
  (resource/inline "examples/tei/tei_example.xml"))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def attr-kmap
  {:xml:lang :lang
   :xml:id   :id})

(def da-type
  {"conference" "Konference"
   "org"        "Organisation"
   "pers"       "Person"
   "place"      "Sted"
   "publ"       "Publikation"
   "receiver"   "Modtager"
   "sender"     "Afsender"})

(def pred->comp
  {(comp #{:tei-list} first) (fn [this]
                               [:ul
                                (for [child (array-seq (.-children this))]
                                  [:li {:dangerouslySetInnerHTML {:__html (.-innerHTML child)}
                                        :key                     (hash child)}])])
   (comp :data-ref second)   (fn [this]
                               (let [dataset   (.-dataset this)
                                     href      (.-ref dataset)
                                     href-type (da-type (.-type dataset))]
                                 [:a {:href  href
                                      :title href-type}
                                  [:slot]]))})

(defn algorithmic-rewrite
  [node]
  (let [matching-comp #(fn [_ pred comp]
                         (when (pred %)
                           (reduced comp)))]
    (reduce-kv (matching-comp node) nil pred->comp)))

(defn meander-rewrite*
  [x]
  (m/rewrite x
    [:list (m/or {:as ?attr}
                 (m/let [?attr {}])) .
     ;; TODO: fix - only works on :item with exactly 1 piece of content
     [:item {:as _} !x] ...]
    [:ul ?attr .
     [:li !x] ...]

    [_ {:ref  (m/some ?ref)
        :type ?type} & _]
    [:a {:href  ?ref
         :title (m/app da-type ?type)}
     [:slot]]))

(defn hiccup->comp
  [hiccup]
  (when hiccup
    (fn [this] hiccup)))

(def meander-injector
  (comp hiccup->comp meander-rewrite*))

(defonce css-href
  (interop/auto-revoked (atom nil)))

(defn app
  []
  (let [css        (style/prefix-css "tei" css-example)
        hiccup     (-> (xml/parse tei-example)
                       (hic/postprocess {:prefix    "tei"
                                         :attr-kmap attr-kmap
                                         :injectors [meander-injector]}))
        teiheader  (select/one hiccup (select/element :tei-teiheader))
        facsimile  (select/one hiccup (select/element :tei-facsimile))
        text       (select/one hiccup (select/element :tei-text))
        test-nodes (select/all hiccup
                               (select/element :tei-forename)
                               (select/attr {:data-type "first"}))]
    (reset! css-href (interop/blob-url [css] {:type "text/css"}))
    [:<>
     [:fieldset
      [:legend "Document"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint hiccup))]]
      [:details
       [:summary "CSS"]
       [:pre css]]
      ;; This is just left here as a proof of concept. Using <link> over <style>
      ;; in a shadow DOM introduces a bit of flickering in Chrome and Firefox,
      ;; which is not desirable. Safari seems unaffected, though.
      ;[rescope/scope hiccup [:link {:rel "stylesheet" :href @css-href}]]]
      [rescope/scope hiccup css]]
     [:fieldset
      [:legend "Header"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint teiheader))]]]
     [:fieldset
      [:legend "Facsimile"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint facsimile))]]]
     [:fieldset
      [:legend "Text"]
      [:details
       [:summary "Hiccup"]
       [:pre (with-out-str (pprint text))]]]
     [:fieldset
      [:legend "Test output"]
      [:pre (with-out-str (pprint test-nodes))]]]))

(def root
  (js/document.getElementById "app"))

(defn ^:dev/after-load render
  []
  (rdom/render [app] root))

(defn start-dev
  []
  (println "Started development environment for kuhumcst/rescope.")
  (render))
