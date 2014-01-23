(ns warden.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer (html) :include-macros true]
            [warden.font-awesome :refer (font-icon)]
            [warden.pure :refer (responsive-grid menu grid-unit grid-row)]))

(declare app process supervisor)

(defn supervisor-err
  "Render the error section for an unreachable supervisor"
  [{:keys [host name port] {err :fault-string} :state}]
  (let [error (str "Could not connect to " name " at " host ":" port " - " err)]
    (grid-row [:section.supervisor.error
               (font-icon :exclamation-triangle 2)
               error])))

(defn supervisor-ok [{:keys [host port name processes pid state id version]}]
  (let [public-url (str "http://" host ":" port)
        supervisord-description (str name " - " host)
        supervisord-state (str (:statename state) " - " (count processes) " processes")]
    [:section.supervisor
     (responsive-grid [:header
         (grid-unit 5 6 [:span.description
                          [:a {:href public-url
                               :target "_blank"} supervisord-description]])
         (grid-unit 1 6 [:span.state supervisord-state])])
     (responsive-grid [:ul.processes (map process processes)])]))

(defn supervisor [supervisor]
  (if (get-in supervisor [:state :fault-string])
    (supervisor-err supervisor)
    (supervisor-ok supervisor)))

(defn process [{:keys [name description]}]
  (grid-row [:li (str name" - " description)]))

(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name description]} state]
    (om/component
     (html
      (responsive-grid [:div.main
                   (menu [:header
                (grid-unit [:h2 name])
                (grid-unit [:h3.description description])])
             (grid-row [:div.supervisors (map supervisor supervisors)])])))))
