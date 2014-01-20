(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer [html] :include-macros true]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def app-state (atom {:data "Loading..."
                      :heartbeat {:enabled true
                                  :wait 5000}}))

(defn poll! [url]
  (go-loop [response (<! (http/get url))]
    (swap! app-state assoc :data (:body response))
    (let [{{:keys [wait enabled]} :heartbeat} @app-state]
      (when enabled
        (<! (timeout wait))
        (recur (<! (http/get url)))))))

(defn app [state]
  (let [{data :data} state]
    (om/component
      (html [:h1 data]))))

(defn ^:export start []
  (poll! "/api/supervisors")
  (om/root app-state app js/document.body))
