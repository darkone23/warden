(ns warden.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer (html) :include-macros true]
            [warden.helpers :refer (add-class external-link hide-on-phone menu
                                    font-icon responsive-grid grid-unit grid-row)]))

(declare app process supervisor)

(defn supervisor-err
  "Render the error section for an unreachable supervisor"
  [{:keys [host name port] {err :fault-string} :state}]
  (let [message (str "Could not connect to " name " at " host ":"  port)]
    (grid-row [:section.supervisor.error
               (grid-unit [:span.message message])
               (grid-unit [:code.err err])
               (font-icon :exclamation-triangle 2)])))

(defn supervisor-ok
  "Render an element representing a supervisord instance "
  [{:keys [host port name processes pid state id version] :as s} config]
  (let [public-url (str "http://" host ":" port)
        description (str name "@" host)
        state (:statename state)
        showing? (get (:showing @config) name)]
    [:section.supervisor
     (responsive-grid
      [:header
       (grid-unit 5 6 [:span.description
                       (external-link public-url description)])
       (hide-on-phone
        (grid-unit 1 6 [(add-class :span.state state)
                        [:span.process-count (count processes)]
                        (font-icon :eye)]))])
     (let [ul (add-class :ul.process (if showing? :showing :hidden))]
       (responsive-grid
        [ul (map process processes (repeat config))]))]))

(defn supervisor [supervisor config]
  (if (get-in supervisor [:state :fault-string])
    (supervisor-err supervisor)
    (supervisor-ok supervisor config)))

(defn process [{:keys [name statename description]} config]
  (grid-row [(add-class :li.process statename)
    (grid-unit [:span.name name])
    (grid-unit [:span.description description])
    (grid-unit [:span.state statename])]))

(defn app [state]
  "App as a function of application state"
  (let [{:keys [config supervisors name description]} state]
    (om/component
     (html
      (responsive-grid
       [:div.main
        (menu
         [:header
          (grid-unit [:h2 name])
          (-> [:h3.description description] grid-unit hide-on-phone)])
        (grid-row
         [:div.supervisors (map supervisor supervisors (repeat config))])])))))
