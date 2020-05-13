(ns kuhumcst.rescope.cuphic-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [kuhumcst.rescope.cuphic :as cup]))

(def hiccup-example
  '[:div {:class "class"
          :id    "id"
          :style {:width  "10px"
                  :height "20px"}}
    [:p {:on-click do-stuff}
     "text"]
    [:p "more text" 1 2 3]
    [:p "text"
     [:span "x"]
     [:em "y"]]])

;; Includes _ (ignored values), meaning it can only be a "from" template.
(def from
  '[?div {:class ?class
          :id    _
          :style {:width  "10px"
                  :height "20px"}}
    [:p {:on-click do-stuff}
     "text"]
    [?p "more text" 1 2 3]
    [:p "text"
     [?span "x"]
     [:em "y"]]])

(def symbol->value
  '{?div   :div
    ?class "class"
    ?p     :p
    ?span  :span})

(deftest spec-validation
  (testing "slots (insertion points for symbols)"
    (is (= [:var '?var]
           (s/conform ::cup/slot '?var)))
    (is (= [:ignored '_ignored]
           (s/conform ::cup/slot '_ignored)))
    (is (= [:other 'symbol]
           (s/conform ::cup/slot 'symbol)))

    (is (= [:other "string"]
           (s/conform ::cup/slot "string")))
    (is (not (s/valid? ::cup/slot [:div]))))

  (testing "complex hiccup"
    (is (= '{:tag     [:other :div]
             :attr    {:class [:slot [:other "class"]]
                       :id    [:slot [:other "id"]]
                       :style [:map {:width  [:slot [:other "10px"]]
                                     :height [:slot [:other "20px"]]}]}
             :content [[:cuphic {:tag     [:other :p]
                                 :attr    {:on-click [:slot [:other do-stuff]]}
                                 :content [[:other "text"]]}]
                       [:cuphic {:tag     [:other :p]
                                 :content [[:other "more text"]
                                           [:other 1]
                                           [:other 2]
                                           [:other 3]]}]
                       [:cuphic
                        {:tag     [:other :p],
                         :content [[:other "text"]
                                   [:cuphic {:tag     [:other :span]
                                             :content [[:other "x"]]}]
                                   [:cuphic {:tag     [:other :em]
                                             :content [[:other "y"]]}]]}]]}
           (s/conform ::cup/cuphic hiccup-example))))

  (testing "complex cuphic"
    (is (= '{:tag     [:var ?div]
             :attr    {:class [:slot [:var ?class]]
                       :id    [:slot [:ignored _]]
                       :style [:map {:width  [:slot [:other "10px"]]
                                     :height [:slot [:other "20px"]]}]}
             :content [[:cuphic {:tag     [:other :p]
                                 :attr    {:on-click [:slot [:other do-stuff]]}
                                 :content [[:other "text"]]}]
                       [:cuphic {:tag     [:var ?p]
                                 :content [[:other "more text"]
                                           [:other 1]
                                           [:other 2]
                                           [:other 3]]}]
                       [:cuphic
                        {:tag     [:other :p],
                         :content [[:other "text"]
                                   [:cuphic {:tag     [:var ?span]
                                             :content [[:other "x"]]}]
                                   [:cuphic {:tag     [:other :em]
                                             :content [[:other "y"]]}]]}]]}
           (s/conform ::cup/cuphic from)))))

(deftest resemble
  (testing "complex hiccup"
    (is (cup/same-shape? hiccup-example
                         [nil {}
                          [nil {}
                           nil]
                          [nil nil nil nil nil]
                          [nil nil
                           [nil nil]
                           [nil nil]]])))

  (testing "complex cuphic"
    (is (cup/same-shape? from
                         [nil {}
                          [nil {}
                           nil]
                          [nil nil nil nil nil]
                          [nil nil
                           [nil nil]
                           [nil nil]]])))

  (testing "legal insertion (attr)"
    (is (cup/same-shape? [:div {:class "class"
                                :id    "id"}]
                         '[:div {:class    "class"
                                 :id       "id"
                                 :on-click do-stuff}])))

  (testing "illegal insertion (content)"
    (is (not (cup/same-shape? [:div {:class "class"
                                     :id    "id"}]
                              [:div {:class "class"
                                     :id    "id"}
                               "text"])))))

(deftest logic-vars
  (testing "basic mapping"
    (is (= symbol->value
           (cup/logic-vars from hiccup-example)))))

(deftest transform
  (testing "preservation"
    (is (= hiccup-example
           (cup/transform from
                          '[?div {:class ?class
                                  :id    "id"
                                  :style {:width  "10px"
                                          :height "20px"}}
                            [:p {:on-click do-stuff}
                             "text"]
                            [?p "more text" 1 2 3]
                            [:p "text"
                             [?span "x"]
                             [:em "y"]]]
                          hiccup-example))))

  (testing "cuphic/cuphic transformation"
    (is (= [:div [:p] [:span {:class "class"}]]
           (cup/transform from
                          '[?div [?p] [?span {:class ?class}]]
                          hiccup-example))))

  (testing "fn/cuphic transformation"
    (is (= [:div [:p] [:span {:class "class"}]]
           (cup/transform (fn [hiccup] symbol->value)
                          '[?div [?p] [?span {:class ?class}]]
                          hiccup-example))))

  (testing "cuphic/fn transformation"
    (is (= [:div [:p] [:span {:class "class"}]]
           (cup/transform from
                          (fn [{:syms [?div ?p ?span ?class]}]
                            [?div [?p] [?span {:class ?class}]])
                          hiccup-example))))

  (testing "fn/fn transformation"
    (is (= [:div [:p] [:span {:class "class"}]]
           (cup/transform (fn [hiccup] symbol->value)
                          (fn [{:syms [?div ?p ?span ?class]}]
                            [?div [?p] [?span {:class ?class}]])
                          hiccup-example)))))