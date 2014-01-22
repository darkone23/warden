(ns warden.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer (html) :include-macros true]
            [warden.pure :refer (responsive-grid menu grid-unit grid-row)]))

(declare app process supervisor)

(defn has-error? [{host :host port :port {err :fault-string} :state}] err)

(defn supervisor-err [{:keys [host name port] {err :fault-string} :state}]
  (grid-row [:section.supervisor.error
             (str "Could not connect to " name " at " host ":" port " - " err)]))

(defn supervisor-ok [{:keys [host port name processes pid state id version]}]
  (let [public-url (str "http://" host ":" port)
        supervisord-description (str name " - " host)
        supervisord-state (str (:statename state) " - " (count processes) " processes")]
    [:section.supervisor
     (responsive-grid [:header
         (grid-unit 5 6 [:span.description
                          [:a {:href public-url :target "_blank"} supervisord-description]])
         (grid-unit 1 6 [:span.state supervisord-state])])
     (responsive-grid [:ul.processes (map process processes)])]))

(defn supervisor [supervisor]
  (if (has-error? supervisor)
    (supervisor-err supervisor)
    (supervisor-ok supervisor)))

(defn process [{:keys [name description]}]
  (grid-row [:li (str name" - " description)]))

(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name description]} state]
    (-> (responsive-grid [:div.main
          (menu            [:header (grid-unit [:h2 name])
                                    (grid-unit [:h3.description description])])
          (grid-row      [:div.supervisors (map supervisor supervisors)])])
        html om/component)))
