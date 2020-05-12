(ns kuhumcst.rescope.cuphic-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [kuhumcst.rescope.cuphic :as cup]))

(def complex-hiccup
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

(def complex-cuphic
  '[?div {:class ?class
          :id    "id"
          :style {:width  "10px"
                  :height "20px"}}
    [:p {:on-click do-stuff}
     "text"]
    [?p "more text" 1 2 3]
    [:p "text"
     [?span "x"]
     [:em "y"]]])

(deftest spec-validation
  (testing "slots (insertion points for symbols)"
    (is (= [:var '?logic-variable]
           (s/conform ::cup/slot '?logic-variable)))
    (is (= [:other 'symbol]
           (s/conform ::cup/slot 'symbol)))
    (is (= [:other "string"]
           (s/conform ::cup/slot "string")))
    (is (s/invalid? (s/conform ::cup/slot [:div]))))

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
           (s/conform ::cup/cuphic complex-hiccup))))

  (testing "complex cuphic"
    (is (= '{:tag     [:var ?div]
             :attr    {:class [:slot [:var ?class]]
                       :id    [:slot [:other "id"]]
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
           (s/conform ::cup/cuphic complex-cuphic)))))

(deftest resemble
  (testing "complex hiccup"
    (is (cup/resemble complex-hiccup
                      [nil {}
                       [nil {}
                        nil]
                       [nil nil nil nil nil]
                       [nil nil
                        [nil nil]
                        [nil nil]]])))

  (testing "complex cuphic"
    (is (cup/resemble complex-cuphic
                      [nil {}
                       [nil {}
                        nil]
                       [nil nil nil nil nil]
                       [nil nil
                        [nil nil]
                        [nil nil]]])))

  (testing "legal insertion (attr)"
    (is (cup/resemble [:div {:class "class"
                             :id    "id"}]
                      '[:div {:class    "class"
                              :id       "id"
                              :on-click do-stuff}])))

  (testing "illegal insertion (content)"
    (is (not (cup/resemble [:div {:class "class"
                                  :id    "id"}]
                           [:div {:class "class"
                                  :id    "id"}
                            "text"])))))
