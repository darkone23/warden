(ns warden.components.processes
  (:require [warden.net :refer (cljson-post)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer (chan put!)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defn handle-error! [ch response]
  "Handle responses from the server for POSTing an action"
  (case (:status response)
    201 (if-let [err (some-> (update-in response [:body] cljson->clj)
                             (get-in [:body :result :fault-string]))]
          (put! ch [:error err]))
    404 (if-let [err (:body response)] (put! ch [:error err]))))

(defn handle-action! [ch name action url]
  "Handle a process action from the user"
  (go
   (let [message (str "sent: " action " " name)]
     (put! ch [:message message])
     (handle-error! ch (<! (cljson-post url))))))

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
      (let [[supervisor-api action-chan super-chan]
              (map (fn [k] (om/get-state owner k))
                [:supervisor-api :action-chan :super-chan])
            [start stop restart]
              (map (fn [a] (str supervisor-api "/processes/" name "/action/" a))
                ["start", "stop" "restart"])]
        (go-loop [[k v] (<! action-chan)]
          (case k
            ::start   (handle-action! super-chan name "start"   start)
            ::stop    (handle-action! super-chan name "stop"    stop)
            ::restart (handle-action! super-chan name "restart" restart))
          (recur (<! action-chan)))))))

(defn processes [processes owner]
  "Collection of supervised processes"
  (om/component
    (apply dom/ul #js {:className "processes"}
      (for [{:keys [pid name] :as p} processes]
        (om/build process p
          {:init-state (om/get-state owner)
           :react-key (str pid name)})))))
