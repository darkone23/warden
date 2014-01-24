(ns warden.core
  (:require [om.core :as om :include-macros true]
            [alandipert.storage-atom :refer (local-storage)]
            [cljs.core.async :refer (chan)]
            [warden.components :refer (app)]
            [warden.net :refer (poll! sync-state!)]))

(defn ^:export start []
  (let [poll-ch (chan 1)
        config (local-storage (atom {}) :config)
        app-state (atom {:supervisors []
                          :config config
                          :name "warden"
                          :description "process management"})]
    (poll! "/api/supervisors" 2500 poll-ch)
    (sync-state! poll-ch app-state [:supervisors])
    (om/root app-state app js/document.body)))
