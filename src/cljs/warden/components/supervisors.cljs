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
  (let [acceptable-states #{"RUNNING" "STARTING" "STOPPED"}]
    (every? (comp acceptable-states :statename) processes)))

(defn showing? [config super]
  "did a user intend to show this server in detail?"
  (get (:showing @config) (supervisor-id super)))

(defn schedule-disj! [owner korks v time]
  "Schedules a value to be disjoined from a om state set"
  (js/setTimeout
    #(let [old-set (om/get-state owner korks)
           new-set (disj old-set v)]
       (om/set-state! owner korks new-set))
   time))

(defn supervisor-ok [{:keys [url state-name description] :as super} owner]
  "Representation of a supervisor server
   Relays UI events to parent core.async channel"
  (reify
    om/IRenderState
    (render-state [this {:keys [messages errors super-chan config]}]
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
             (dom/span #js {:className "pure-u-1-2"}
               (dom/span #js {:className (str "state " state-name)}
                 (dom/span #js {:className "process-count"} (count procs))
                 (dom/i #js {:className (str "fa " health-icon-class)}))
               (dom/span #js {:className "description"}
                 (dom/a #js {:href url :target "_blank"} description)))
             (apply dom/span #js {:className "messages pure-u-1-3"}
               (concat (for [m messages] (dom/span #js {:className "message"} m))
                       (for [e errors] (dom/span #js {:className "error"} e))))
             (dom/span #js {:className "controls pure-u-1-6"}
               (dom/i #js {:className (str "fa " showing-icon-class)})))
          (om/build processes procs
            {:init-state (om/get-state owner)}))))

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :super-chan)
            config (om/get-state owner :config)]
        (go-loop [[k v] (<! ch)]
          (let [id (supervisor-id v)]
            (case k
              :message
                (let [messages (om/get-state owner [:messages])]
                  (om/set-state! owner [:messages] (conj messages v))
                  (schedule-disj! owner [:messages] v 2500))
              :error
                (let [errors (om/get-state owner [:errors])]
                  (om/set-state! owner [:messages] #{})
                  (om/set-state! owner [:errors] (conj errors v))
                  (schedule-disj! owner [:errors] v 2500))
              ::toggle-showing
                (if (showing? config v)
                  (swap! config update-in [:showing] disj id)
                  (swap! config update-in [:showing] conj id)))
            (om/transact! super identity))
          (recur (<! ch)))))))

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
    (init-state [this]
      {:super-chan (chan 1)
       :errors #{}
       :messages #{}})))

(defn supervisor-api [{:keys [host name]}]
  (str "/api/supervisors/" host "/" name))

(defn supervisors [state owner]
  "Collection of supervisor servers"
  (om/component
    (apply dom/div #js {:className "supervisors pure-u-1"}
      (for [super (:supervisors state)]
        (om/build supervisor super
          {:init-state (assoc (om/get-state owner) :supervisor-api (supervisor-api super))
           :react-key (supervisor-id super)
           :fn (fn [{:keys [host port name state] :as super}]
                 (merge super
                   {:url (str "http://" host ":" port)
                    :state-name (:statename state)
                    :description (str name "@" host)}))})))))
