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

(declare process supervisor)
(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name]} state]
    (om/component
      (html [:div.main
             [:header [:h1 name]]
             [:div.supervisors (map supervisor supervisors)]]))))

(defn supervisor [{:keys [host port name processes pid state id version]}]
  (let [public-url (str "http://" host ":" port)
        supervisord-description (str name " - " id " v" version " - " pid)
        supervisord-state (str (:statename state) " - " (count processes) " processes")]
    [:section.supervisor
     [:h4 [:a {:href public-url :target "_blank"} supervisord-description]]
     [:span.state supervisord-state]
     [:ul.processes (map process processes)]]))

(defn process [{:keys [name description]}]
  [:li (str name" - " description)])

(defn ^:export start []
  (let [poll-ch (chan 1)
        app-state (atom {:supervisors []
                         :name "Warden"})]
    (poll! "/api/supervisors" 2500 poll-ch)
    (sync-state! poll-ch app-state [:supervisors])
    (om/root app-state app js/document.body)))
