(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer (html) :include-macros true]
            [warden.pure :refer (grid-unit responsive-grid)]
            [warden.net :refer (poll! sync-state!)]
            [cljs.core.async :refer (chan)]))

(declare process supervisor)

(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name]} state]
    (-> (responsive-grid [:div.main
          (grid-unit [:header [:h1 name]])
          (grid-unit [:div.supervisors (map supervisor supervisors)])])
        html om/component)))

(defn supervisor [{:keys [host port name processes pid state id version]}]
  (let [public-url (str "http://" host ":" port)
        supervisord-description (str name " - " id " v" version " - " pid)
        supervisord-state (str (:statename state) " - " (count processes) " processes")]
    [:section.supervisor
     [:h4 [:a {:href public-url
               :target "_blank"}
           supervisord-description]]
     [:span.state supervisord-state]
     [:ul.processes (map process processes)]]))

(defn process [{:keys [name description]}]
  [:li (str name" - " description)])

(defn ^:export start []
  (let [poll-ch (chan 1)
        app-state (atom {:supervisors []
                         :name "Warden"})]
    (poll! "/api/supervisors" 2500 poll-ch)
    (sync-state! poll-ch app-state [:supervisors])
    (om/root app-state app js/document.body)))
