(ns kuhumcst.facsimile.style-test
  (:require [clojure.test :refer [deftest is]]
            [shadow.resource :as resource]
            [kuhumcst.facsimile.style :as style]))

(def css-example
  (resource/inline "examples/css/tei.css"))

(def css-patched-example
  (resource/inline "examples/css/tei_patched.css"))

;; Helper function for debugging patch-css unit test.
(defn- str-divergence
  [s1 s2]
  (loop [shared ""
         [c1 & r1] s1
         [c2 & r2] s2]
    (if (not= c1 c2)
      [shared (apply str c1 r1) (apply str c2 r2)]
      (recur (str shared c1) r1 r2))))

(deftest patch-css
  (let [actual   (style/patch-css "tei" css-example)
        expected css-patched-example]
    #_(cljs.pprint/pprint (str-divergence actual expected))
    (is (= actual expected))))
