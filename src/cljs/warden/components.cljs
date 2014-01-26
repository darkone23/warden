(ns warden.components
  (:require [om.core :as om :include-macros true]
            [alandipert.storage-atom :refer [local-storage]]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan sliding-buffer >! put!)]
            [warden.net :refer (poll! sync-state!)])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn process [{:keys [name statename description]} owner]
  (om/component
   (dom/li #js {:className (str statename " process pure-u-1")}
     (dom/span #js {:className "name pure-u"} name)
     (dom/span #js {:className "description pure-u"} description)
     (dom/span #js {:className "state pure-u"} statename))))

;; helper fns for working with supervisors
(defn supervisor-id [{:keys [host name port]}]
  (str host name port))

(defn healthy? [{:keys [processes]}]
  (let [acceptable-states #{"RUNNING" "EXITED"}]
    (every? (comp acceptable-states :statename) processes)))

(defn showing? [config super]
  (get (:showing @config) (supervisor-id super)))

(defn supervisor-ok [{:keys [url state-name description processes] :as super} owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [super-chan config]}]
      (let [health (healthy? super)
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
              (dom/span #js {:className "process-count"} (count processes))
              (dom/i #js {:className (str "fa " health-icon-class)})))
          (apply dom/ul #js {:className "process"}
            (om/build-all process processes)))))))

(defn supervisor-err [{:keys [host name port] {err :fault-string} :state}]
  (let [message (str "Could not connect to " name " at " host ":"  port)]
    (dom/section #js {:className "supervisor error pure-u-1"}
      (dom/span #js {:className "message pure-u"} message)
      (dom/code #js {:className "err pure-u"} err)
      (dom/i #js {:className "fa fa-eye-slash fa-2x"}))))

(defn supervisor [state owner]
  (reify
    om/IInitState
    (init-state [this] {:super-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :super-chan)
            config (om/get-state owner :config)]
        (go-loop [[k v] (<! ch)]
          (let [id (supervisor-id v)]
            (when (= k ::toggle-showing)
              (if (showing? config v)
                (swap! config update-in [:showing] disj id)
                (swap! config update-in [:showing] conj id))
              (om/transact! state identity)))
          (recur (<! ch)))))

    om/IRenderState
    (render-state [this s]
      (if (get-in state [:state :fault-string])
        (supervisor-err state)
        (om/build supervisor-ok state
           {:init-state s})))))

(defn supervisors [state owner]
  (om/component
    (apply dom/div #js {:className "supervisors pure-u-1"}
      (for [super (:supervisors state)]
        (om/build supervisor super
          {:init-state (om/get-state owner)
           :fn (fn [{:keys [host port name state] :as super}]
                 (merge super
                   {:url (str "http://" host ":" port)
                    :state-name (:statename state)
                    :description (str name "@" host)}))})))))

(defn header-menu [state owner]
  (om/component
   (dom/header #js {:className "pure-menu pure-menu-fixed pure-menu-horizontal"}
    (dom/h2 #js {:className "pure-u"} (:name state))
    (dom/h3 #js {:className "description pure-u"} (:description state)))))

(defn app [state owner]
  "App as a function of application state"
  (reify
    om/IInitState
    (init-state [this]
      {:config (local-storage (atom {:showing #{}}) :config)
       :api-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :api-chan)]
        (poll! "/api/supervisors" 2500 ch)
        (sync-state! ch owner [:supervisors])))

    om/IRender
    (render [this]
      (dom/div #js {:className "main pure-g-r"}
        (om/build header-menu state)
        (om/build supervisors state
          {:init-state (om/get-state owner)})))))
