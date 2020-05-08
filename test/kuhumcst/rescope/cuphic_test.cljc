(ns kuhumcst.rescope.cuphic-test
  (:require [clojure.test :refer [deftest is testing]]
            [kuhumcst.rescope.cuphic :as cup]))

(deftest resemble
  (testing "complex shape"
    (is (cup/resemble '[:div {:class "class"
                              :id    "id"
                              :style {:width  "10px"
                                      :height "20px"}}
                        [:p {:on-click do-stuff}
                         "text"]
                        [:p "more text" 1 2 3]
                        [:p "text"
                         [:span "x"]
                         [:em "y"]]]
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
