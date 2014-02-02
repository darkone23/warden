(ns warden.core
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [secretary.core :as secretary]
            [warden.components.core :refer (app)])
  (:require-macros [secretary.core :refer (defroute)])
  (:import [goog History]
           [goog.history EventType]))

(def app-state (atom {:name "warden"
                      :description "process management"
                      :route :home
                      :supervisors []}))

(defroute "/" []
;  (swap! app-state assoc :route :home)
  (swap! app-state assoc :route :supervisors))

(defroute "/supervisors" []
  (swap! app-state assoc :route :supervisors))

(defroute "/processes" []
  (swap! app-state assoc :route :processes))

(defroute "/supervisors/:host/:name" [host name]
  (swap! app-state assoc
    :route :supervisors
    :route-params {:host host :name name}))

(defroute "/supervisors/:host/:name/:process" [host name process]
  (swap! app-state assoc
    :route :supervisors
    :route-params {:host host :name name :process process}))

(def history (History.))

(events/listen history
  EventType.NAVIGATE (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)

(defn ^:export start []
  (om/root app-state app js/document.body))
