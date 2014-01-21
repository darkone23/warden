(ns warden.core-test
  (:use [warden.core :only [supervisor-node]]
        [purnam.cljs :only [aget-in aset-in js-equals]])
  (:use-macros [purnam.js :only [! ? f.n def.n obj arr]]
               [purnam.test :only [init describe is it]]
               [purnam.test.sweet :only [fact facts]]))

(init) ;; start the test runner

(def test-s {:pid :pid
             :processes []
             :state {:statename :state}
             :id :id
             :name :name})

(facts "Dom representations can be generated"
  (fact "supervisor nodes!"
    (supervisor-node test-s) => vector?))
