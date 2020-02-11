(ns kuhumcst.facsimile.util-test
  (:require [clojure.test :refer [deftest is]]
            [kuhumcst.facsimile.util :as util]))

(deftest prefixed
  (let [prefixed* (partial util/prefixed "font")
        actual    (map prefixed* ["div" "my-custom-tag" "face"])
        expected  ["font-div" "font-my-custom-tag" "font-face-x"]]
    (is (= actual expected))))

(deftest data-*
  (let [actual   (map util/data-* ["rend" "xml:lang" "id"])
        expected ["data-rend" "data-xml-lang" "data-id"]]
    (is (= actual expected))))
