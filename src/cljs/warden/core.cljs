(ns warden.core
  (:require [om.core :as om :include-macros true]
            [warden.components :refer (app)]))

(defn ^:export start []
  (let [app-state (atom {:name "warden"
                         :description "process management"
                         :supervisors []})]
    (om/root app-state app js/document.body)))
