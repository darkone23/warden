(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer [html] :include-macros true]
            [cljs-http.client :as http]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def app-state (atom {:supervisors []
                      :name "Warden"
                      :heartbeat {:enabled true
                                  :wait 5000}}))

(defn poll-supervisors! [url]
  "Badly needs refactored for re-use, om component perhaps?"
  (go-loop [response (<! (http/get url))]
    (swap! app-state assoc :supervisors (-> response :body cljson->clj))
    (let [{{:keys [wait enabled]} :heartbeat} @app-state]
      (when enabled
        (<! (timeout wait))
        (recur (<! (http/get url)))))))

(defn app [state]
  "App as a function of application state"
  (let [{:keys [supervisors name]} state]
    (om/component
      (html [:div.main
             [:header [:h1 name]]
             [:div.supervisors
                (for [supervisor supervisors]
                  [:section.supervisor
                    [:h2 (:name supervisor)]
                    [:span.state (-> supervisor :state :statename)]])]]))))

(defn ^:export start []
  (poll-supervisors! "/api/supervisors")
  (om/root app-state app js/document.body))
