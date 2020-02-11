(ns kuhumcst.facsimile.style-test
  (:require [clojure.test :refer [deftest is]]
            [shadow.resource :as resource]
            [kuhumcst.facsimile.style :as style]))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def css-patched-example
  (resource/inline "examples/css/tei_patched.css"))

(deftest patch-css
  (let [actual   (style/patch-css "tei" css-example)
        expected css-patched-example]
    (is (= actual expected))))
