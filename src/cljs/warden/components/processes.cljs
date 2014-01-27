(ns warden.components.processes
  (:require [warden.net :refer (cljson-post)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan put!)])
  (:require-macros [cljs.core.async.macros :refer (go-loop)]
                   [cljs.core.match.macros :refer (match)]))

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
                      :onClick #(put! action-chan [::stop @p])}))))

    om/IInitState
    (init-state [this]
      {:action-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [process-api (str (om/get-state owner :supervisor-api) "/processes/" name)
            start-api (str process-api "/action/start")
            stop-api (str process-api "/action/stop")
            ch (om/get-state owner :action-chan)]
        (go-loop [[k _] (<! ch)]
          (match [k]
            [::start] (cljson-post start-api)
            [::stop] (cljson-post stop-api))
          (recur (<! ch)))))))

(defn processes [processes owner]
  "Collection of supervised processes"
  (om/component
    (apply dom/ul #js {:className "processes"}
      (for [{:keys [pid name] :as p} processes]
        (om/build process p
          {:init-state (om/get-state owner)
           :react-key (str pid name)})))))
