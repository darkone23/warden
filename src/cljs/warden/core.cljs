(ns warden.core
  (:require [om.core :as om :include-macros true]
            [alandipert.storage-atom :refer (local-storage)]
            [warden.components :refer (app)]))

(defn ^:export start []
  (let [config (local-storage
                 (atom {:showing #{}}) :config)
        app-state (atom {:name "warden"
                         :description "process management"
                         :supervisors []
                         :config config})]
    (om/root app-state app js/document.body)))
