(ns warden.core
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [secretary.core :as secretary :include-macros true :refer (defroute)]
            [warden.components.core :refer (app)])
  (:import [goog History]
           [goog.history EventType]))

(def app-state (atom {:name "warden"
                      :description "process management"
                      :fn identity
                      :route :home
                      :supervisors []}))

(defroute "/" []
  (swap! app-state assoc :route :home))

(defroute "/supervisors" []
  (swap! app-state assoc :route :supervisors))

(defroute "/processes" []
  (swap! app-state assoc :route :processes))

(defroute "/supervisors/:host/:name" {:keys [host name]}
  (swap! app-state assoc
    :route :supervisors
    :route-params {:host host :name name}))

(defroute "/supervisors/:host/:name/:process" {:keys [host name process]}
  (swap! app-state assoc
    :route :supervisors
    :route-params {:host host :name name :process process}))

(def history (History.))

(events/listen history
  EventType.NAVIGATE #(secretary/dispatch (.-token %)))

(.setEnabled history true)

(defn ^:export start []
  (om/root app-state app js/document.body))
