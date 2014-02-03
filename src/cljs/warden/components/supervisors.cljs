(ns warden.components.supervisors
  (:require [warden.components.processes :refer (processes)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan <! put! timeout)])
  (:require-macros [cljs.core.async.macros :refer (go-loop)]))

;; helper fns for working with supervisors
(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn healthy? [{:keys [processes]}]
  "determines whether a supervisor server is healthy"
  (let [info-states #{"STARTING" "EXITING"}
        healthy-states #{"RUNNING" "STOPPED"}]
    (if (every? (comp info-states :statename) processes)
      :info
      (if (every? (comp healthy-states :statename) processes)
        :healthy))))

(defn supervisor-ok [{:keys [url state-name description] :as super} owner]
  "Representation of a supervisor server
   Relays UI events to parent core.async channel"
  (reify
    om/IRender
    (render [this]
      (let [procs (:processes super)
            health (healthy? super)
            health-class (case health
                           :info "info"
                           :healthy "healthy"
                           "unhealthy")
            health-icon-class (case health
                                :info "fa-info-circle"
                                :healthy "fa-eye"
                                "fa-exclamation-triangle")]
        (dom/section #js {:className (str "supervisor pure-u-1-3 " health-class)}
          (dom/header #js {:className "pure-g-r"}
             (dom/span #js {:className "pure-u-1-2"}
               (dom/span #js {:className (str "state " state-name)}
                 (dom/span #js {:className "process-count"} (count procs))
                 (dom/i #js {:className (str "fa " health-icon-class)}))
               (dom/span #js {:className "description"}
                 (dom/a #js {:href url :target "_blank"} description)))))))))

(defn supervisor-err [{:keys [host name port] {err :fault-string} :state}]
  "Unreachable supervisor server"
  (let [message (str name "@" host)]
    (dom/section #js {:className "supervisor error pure-u-1-3"}
      (dom/i #js {:className "fa fa-eye-slash fa-2x"})
      (dom/span #js {:className "message pure-u"} message)
      (dom/code #js {:className "err pure-u"} err))))

(defn supervisor [state owner]
  "Individual supervisor ok|err component
   Owns core.async channel for communicating with sub-components"
  (reify
    om/IRenderState
    (render-state [this s]
      (if (get-in state [:state :fault-string])
        (supervisor-err state)
        (om/build supervisor-ok state
          {:init-state s})))

    om/IInitState
    (init-state [this]
      {:super-chan (chan 1)})))

(defn supervisor-api [{:keys [host name]}]
  (str "/api/supervisors/" host "/" name))

(defn prepare-app-state [app-state owner] app-state)

(defn supervisors [state owner]
  "Collection of supervisor servers"
  (om/component
    (let [state (prepare-app-state state owner)]
      (apply dom/div #js {:className "supervisors pure-u-1 pure-g-r"}
        (for [super (:supervisors state)]
          (om/build supervisor super
            {:react-key (supervisor-id super)
             :fn (fn [{:keys [host port name state] :as super}]
                   (merge super
                     {:url (str "http://" host ":" port)
                      :state-name (:statename state)
                      :description (str name "@" host)}))}))))))
