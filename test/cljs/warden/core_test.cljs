(ns warden.core-test
  (:require [schema.core :as s]
            [warden.schemas :refer (SupervisordInfo)]
            [purnam.cljs :refer [aget-in aset-in js-equals]])
  (:require-macros [schema.macros :as s]
                   [purnam.test :refer [init]]
                   [purnam.test.sweet :refer [fact facts]]))

(init) ;; start the test runner

(def test-s {:processes []
             :state {:statename "RUNNING"
                     :statecode 1}})

(facts "Dom representations can be generated"
  (fact "supervisor nodes!"
    (s/validate SupervisordInfo test-s) => test-s))
