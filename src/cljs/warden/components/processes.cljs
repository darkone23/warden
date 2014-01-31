(ns warden.components.processes
  (:require [warden.net :refer (cljson-post)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer (chan put!)])
  (:require-macros [cljs.core.async.macros :refer (go-loop)]))

(defn handle-action! [ch response]
  "Handle responses from the server for POSTing an action"
  (case (:status response)
    201 (if-let [err (some-> (update-in response [:body] cljson->clj)
                             (get-in [:body :result :fault-string]))]
          (put! ch [:error err]))
    404 (if-let [err (:body response)] (put! ch [:error err]))))

(defn process [{:keys [name statename description] :as p} owner]
  "Single process in a supervisor"
  (reify
    om/IRenderState
    (render-state [this {:keys [action-chan]}]
      (dom/li #js {:className (str statename " process pure-u-1")}
        (dom/span #js {:className "state pure-u"} statename)
        (dom/span #js {:className "name pure-u"} name)
        (dom/span #js {:className "description pure-u"} description)
        (dom/span #js {:className "controls pure-u"}
          (dom/i #js {:className "start fa fa-play"
                      :onClick #(put! action-chan [::start @p])})
          (dom/i #js {:className "stop fa fa-stop"
                      :onClick #(put! action-chan [::stop @p])})
          (dom/i #js {:className "stop fa fa-refresh"
                      :onClick #(put! action-chan [::restart @p])}))))

    om/IInitState
    (init-state [this]
      {:action-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [process-api (str (om/get-state owner :supervisor-api) "/processes/" name)
            restart-api (str process-api "/action/restart")
            start-api (str process-api "/action/start")
            stop-api (str process-api "/action/stop")
            super-chan (om/get-state owner :super-chan)
            action-chan (om/get-state owner :action-chan)]
        (go-loop [[k v] (<! action-chan)]
          (case k
            ::restart
              (let [message (str "sent: restart " (:name v))]
                (put! super-chan [:message message])
                (handle-action! super-chan (<! (cljson-post restart-api))))            
            ::start
              (let [message (str "sent: start " (:name v))]
                (put! super-chan [:message message])
                (handle-action! super-chan (<! (cljson-post start-api))))
            ::stop
              (let [message (str "sent: stop " (:name v))]
                (put! super-chan [:message message])
                (handle-action! super-chan (<! (cljson-post stop-api)))))
          (recur (<! action-chan)))))))

(defn processes [processes owner]
  "Collection of supervised processes"
  (om/component
    (apply dom/ul #js {:className "processes"}
      (for [{:keys [pid name] :as p} processes]
        (om/build process p
          {:init-state (om/get-state owner)
           :react-key (str pid name)})))))
