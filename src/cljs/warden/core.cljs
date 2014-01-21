(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer [html] :include-macros true]
            [cljs-http.client :as http]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer [chan >! <! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn poll! [url wait ch]
  "Poll a url every `wait` ms, delivering responses on ch"
  ;; TODO: handle errors from http/get
  (go-loop [enabled true response (<! (http/get url))]
    (>! ch response)
    (<! (timeout wait))
    (recur enabled (<! (http/get url)))))

(defn sync-state! [ch state cursor]
  "Keep cursor synced with http responses from channel"
  (go-loop [response (<! ch)]
    (when response
      (swap! state assoc-in cursor (-> response :body cljson->clj))
      (recur (<! ch)))))

(defn supervisor [{:keys [name processes pid state id version]}]
  [:section.supervisor
   [:h4 (str name " - " id " v" version " - " pid)]
   [:span.state (str (:statename state) " - " (count processes) " processes")]])

(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name]} state]
    (om/component
      (html [:div.main
             [:header [:h1 name]]
             [:div.supervisors (map supervisor supervisors)]]))))

(defn ^:export start []
  (let [poll-ch (chan 1)
        app-state (atom {:supervisors []
                         :name "Warden"})]
    (poll! "/api/supervisors" 5000 poll-ch)
    (sync-state! poll-ch app-state [:supervisors])
    (om/root app-state app js/document.body)))
