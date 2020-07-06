(ns user
  (:require [clojure.pprint :refer [pprint]]
            [shadow.resource :as resource]
            [reagent.dom :as rdom]
            [meander.epsilon :as m]
            [rescope.formats.xml :as xml]
            [cuphic.core :as cup]
            [rescope.helpers :as helpers]
            [rescope.select :as select]
            [rescope.style :as style]
            [rescope.interop :as interop]
            [rescope.core :as rescope]))
(def tei-example
  ;(resource/inline "examples/tei/1151anno-anno-tei.xml"))
  (resource/inline "examples/tei/tei_example.xml"))
  ;(resource/inline "examples/tei/test-1307-anno-tei.xml"))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def da-type
  {"conference" "Konference"
   "org"        "Organisation"
   "pers"       "Person"
   "place"      "Sted"
   "publ"       "Publikation"
   "receiver"   "Modtager"
   "sender"     "Afsender"})

(defn meander-transformer
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

(def list-as-ul
  (cup/transformer
    :from '[:list +items]
    :to (fn [{:syms [+items]}]
          (into [:ul] (for [[tag attr & content] +items]
                        (into [:li] content))))))

(def ref-as-anchor
  (cup/transformer
    :from '[? {:ref  ?ref
               :type ?type} *]
    :to (fn [{:syms [?ref ?type]}]
          ;; TODO: bug in attr-bindings - now need to check for attr existence
          (when ?ref
            [:a {:href  ?ref
                 :title (da-type ?type)}
             [:slot]]))))

(def wrap-pbs
  (cup/transformer
    :from '[:div * [:<> [:pb] +]]
    :to (fn [{:syms [<>] :as bindings}]
          (let [{:keys [begin end]} (meta <>)
                source (:source (meta bindings))]
            (vec (concat (subvec source 0 begin)
                         [(into [:pbs] (subvec source begin end))]
                         (subvec source end)))))))

(defonce css-href
  (interop/auto-revoked (atom nil)))

(def stages
  [{:transformers [wrap-pbs]}
   {:transformers [ref-as-anchor
                   list-as-ul]
    :wrapper      rescope/shadow-wrapper
    :default      (helpers/default-fn {:attr-kmap {:xml:lang :lang
                                                   :xml:id   :id}
                                       :prefix    "tei"})}])

(defn app
  []
  (let [css        (style/prefix-css "tei" css-example)
        hiccup     (-> (xml/parse tei-example)
                       (cup/rewrite stages))
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
