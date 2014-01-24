(ns warden.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan)]
            [warden.net :refer (poll! sync-state!)]))

(defn process [{:keys [name statename description]} owner]
  (om/component
   (dom/li #js {:className (str statename " process pure-u-1")}
     (dom/span #js {:className "name pure-u"} name)
     (dom/span #js {:className "description pure-u"} description)
     (dom/span #js {:className "state pure-u"} statename))))

(defn supervisor-ok [{:keys [host port name processes state] :as s} owner]
  (let [public-url (str "http://" host ":" port)
        description (str name "@" host)
        state (:statename state)
        showing? true
        show-class (if showing? "showing" "hidden")]
    (dom/section #js {:className "supervisor error pure-u-1"}
      (dom/header #js {:className "pure-g-r"}
        (dom/span #js {:className "description pure-u-5-6"}
          (dom/a #js {:href public-url :target "_blank"} description))
        (dom/span #js {:className (str "state " state " pure-u-1-6 pure-hidden-phone")}
          (dom/span #js {:className "process-count"} (count processes))
          (dom/i #js {:className "fa fa-eye"})))
      (apply dom/ul #js {:className (str "process " show-class)}
        (om/build-all process processes)))))

(defn supervisor-err [{:keys [host name port] {err :fault-string} :state} owner]
  (let [message (str "Could not connect to " name " at " host ":"  port)]
    (dom/section #js {:className "supervisor error pure-u-1"}
      (dom/span #js {:className "message pure-u"} message)
      (dom/code #js {:className "err pure-u"} err)
      (dom/i #js {:className "fa fa-exclamation-triangle fa-2x"}))))

(defn supervisor [state owner]
  (om/component
   (if (get-in state [:state :fault-string])
     (supervisor-err state owner)
     (supervisor-ok state owner))))

(defn supervisors [state owner]
  (om/component
   (apply dom/div #js {:className "supervisors pure-u-1"}
     (om/build-all supervisor (:supervisors state)))))

(defn header-menu [state owner]
  (om/component
   (dom/header #js {:className "pure-menu pure-menu-fixed pure-menu-horizontal"}
    (dom/h2 #js {:className "pure-u"} (:name state))
    (dom/h3 #js {:className "description pure-u pure-hidden-phone"} (:description state)))))

(defn app [state owner]
  "App as a function of application state"
  (reify
    om/IInitState
    (init-state [this]
      {:chans {:supervisors (chan 1)}})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner [:chans :supervisors])]
        (poll! "/api/supervisors" 2500 ch)
        (sync-state! ch owner [:supervisors])))

    om/IRender
    (render [this]
      (dom/div #js {:className "main pure-g-r"}
        (om/build header-menu state)
        (om/build supervisors state)))))
