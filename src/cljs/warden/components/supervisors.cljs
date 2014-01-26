(ns warden.components.supervisors
  (:require [warden.components.processes :refer (processes)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan <! put!)])
  (:require-macros [cljs.core.async.macros :refer (go-loop)]
                   [cljs.core.match.macros :refer (match)]))

;; helper fns for working with supervisors
(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn healthy? [{:keys [processes]}]
  "determines whether a supervisor server is healthy"
  (let [acceptable-states #{"RUNNING" "STOPPED"}]
    (every? (comp acceptable-states :statename) processes)))

(defn showing? [config super]
  "did a user intend to show this server in detail?"
  (get (:showing @config) (supervisor-id super)))

(defn supervisor-ok [{:keys [url state-name description] :as super} owner]
  "Representation of a supervisor server
   Relays UI events to parent core.async channel"
  (reify
    om/IRenderState
    (render-state [this {:keys [super-chan config]}]
      (let [procs (:processes super)
            health (healthy? super)
            health-class (if health "healthy" "unhealthy")
            health-icon-class (if health "fa-eye" "fa-exclamation-triangle")
            showing (showing? config super)
            showing-class (if showing "opened" "closed")
            showing-icon-class (if showing "fa-chevron-up" "fa-chevron-down")]
        (dom/section #js {:className (str "supervisor pure-u-1 " health-class " " showing-class)}
          (dom/header #js {:className "pure-g-r"
                           :onClick #(put! super-chan [::toggle-showing @super])}
            (dom/span #js {:className "description pure-u-5-6"}
              (dom/i #js {:className (str "fa control " showing-icon-class)})
              (dom/a #js {:href url :target "_blank"} description))
            (dom/span #js {:className (str "state " state-name " pure-u-1-6")}
              (dom/span #js {:className "process-count"} (count procs))
              (dom/i #js {:className (str "fa " health-icon-class)})))
          (om/build processes procs
            {:init-state (om/get-state owner)}))))))

(defn supervisor-err [{:keys [host name port] {err :fault-string} :state}]
  "Unreachable supervisor server"
  (let [message (str "Could not connect to " name " at " host ":"  port)]
    (dom/section #js {:className "supervisor error pure-u-1"}
      (dom/span #js {:className "message pure-u"} message)
      (dom/code #js {:className "err pure-u"} err)
      (dom/i #js {:className "fa fa-eye-slash fa-2x"}))))

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
    (init-state [this] {:super-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :super-chan)
            config (om/get-state owner :config)]
        (go-loop [[k v] (<! ch)]
          (let [id (supervisor-id v)]
            (match [k]
              [::toggle-showing]
                (if (showing? config v)
                  (swap! config update-in [:showing] disj id)
                  (swap! config update-in [:showing] conj id))
              :else nil)
            (om/transact! state identity))
          (recur (<! ch)))))))

(defn supervisors [state owner]
  "Collection of supervisor servers"
  (om/component
    (apply dom/div #js {:className "supervisors pure-u-1"}
      (for [super (:supervisors state)]
        (om/build supervisor super
          {:init-state (om/get-state owner)
           :react-key (supervisor-id super)
           :fn (fn [{:keys [host port name state] :as super}]
                 (merge super
                   {:url (str "http://" host ":" port)
                    :state-name (:statename state)
                    :description (str name "@" host)}))})))))
