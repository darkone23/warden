(ns warden.core
  (:require [om.core :as om :include-macros true]
            [warden.components :refer (app)]
            [warden.net :refer (poll! sync-state!)]
            [cljs.core.async :refer (chan)]))

(defn ^:export start []
  (let [poll-ch (chan 1)
        app-state (atom {:supervisors []
                         :name "warden"
                         :description "process management"})]
    (poll! "/api/supervisors" 2500 poll-ch)
    (sync-state! poll-ch app-state [:supervisors])
    (om/root app-state app js/document.body)))
