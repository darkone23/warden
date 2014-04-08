(ns warden.components.processes
  (:require [warden.net :refer (cljson-post)]
            [warden.util :refer (some-key=)]
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

(defn supervisor-api [{:keys [host name]}]
  (str "/api/supervisors/" host "/" name))

(defn process-api [{:keys [supervisor name]}]
  (str (supervisor-api supervisor) "/processes/" name))

(defn process-title [process]
  (let [supervisor-host (get-in process [:supervisor :host])
        supervisor-name (get-in process [:supervisor :name])
        name (:name process)]
    (str name " on " supervisor-name "@" supervisor-host)))

(defn process-detail-url [{{:keys [host name]} :supervisor :as p}]
  (str "#/supervisors/" host "/" name "/" (:name p)))

(defn process [{:keys [statename] :as p} owner]
  "Single process in a supervisor"
  (reify
    om/IRenderState
    (render-state [this {:keys [action-chan]}]
      (dom/li #js {:className (str statename " process pure-u-1")}
        (dom/span #js {:className "state pure-u"} statename)
        (dom/span #js {:className "controls pure-u"}
          (dom/i #js {:className "start fa fa-play"
                      :onClick #(put! action-chan [::start @p])})
          (dom/i #js {:className "stop fa fa-stop"
                      :onClick #(put! action-chan [::stop @p])})
          (dom/i #js {:className "stop fa fa-refresh"
                      :onClick #(put! action-chan [::restart @p])}))
        (dom/a #js {:className "name pure-u" :href (process-detail-url p)} (process-title p))
        (dom/span #js {:className "pure-u"} (:description p))))

    om/IInitState
    (init-state [this]
      {:action-chan (chan 1)
       :message-chan (chan)})

    om/IWillMount
    (will-mount [this]
      (let [action-chan (om/get-state owner :action-chan)
            message-chan (om/get-state owner :message-chan)
            process-api (process-api p)
            [start stop restart]
              (map #(str process-api "/action/" %) ["start", "stop" "restart"])]
        (go-loop [[k v] (<! action-chan)]
          (case k
            ::start   (handle-action! message-chan name "start"   start)
            ::stop    (handle-action! message-chan name "stop"    stop)
            ::restart (handle-action! message-chan name "restart" restart))
          (recur (<! action-chan)))))))

(defn prepare-processes [{supers :supervisors}]
  "Provide each process with select information about its supervisor"
  (mapcat
   (fn [s]
     (if (get-in s [:processes :fault-string])
       [] ;; dont try to render from unreachable supervisors
       (map #(assoc % :supervisor s) (:processes s))))
    supers))

(defn processes [state owner]
  "Collection of supervised processes"
  (om/component
   (let [processes (prepare-processes state)]
     (apply dom/ul #js {:className "processes"}
       (for [{:keys [supervisor name] :as p} processes]
         (om/build process p
           {:init-state (om/get-state owner)
            :react-key (str (:host supervisor) (:name supervisor) name)}))))))

(defn get-process [{{host :host sname :name pname :process} :route-params supers :supervisors}]
  (if-let [s (some-key= {:host host :name sname} supers)]
    (if-let [p (some-key= {:name pname} (:processes s))]
      (assoc p :supervisor s)
      {:error (str "Could not find process named '" pname "'")})
    {:error (str "Could not find supervisor named '" sname "' on host " host)}))

(defn process-detail [state owner]
  (let [p (get-process state)]
    (om/component
     (om/build process p {:init-state (om/get-state owner)}))))
