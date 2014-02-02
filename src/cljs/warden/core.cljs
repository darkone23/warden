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

(defroute "/:route" [route]
  (swap! app-state assoc :route (keyword route)))

(def history (History.))

(events/listen history EventType.NAVIGATE
  (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)

(defn ^:export start []
  (om/root app-state app js/document.body))
