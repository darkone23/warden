(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer [html] :include-macros true]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def app-state (atom {:route :home
                      :name "warden"}))

(def wait 5000)

(defn load! [url]
  (go-loop [response (<! (http/get url))]
    (let [route (:route @app-state)]
      (swap! app-state assoc :name (:body response))
      (<! (timeout wait))
      (if (= :home route)
        (recur (<! (http/get url)))))))

(defn app [state]
  (let [{name :name} state
        greeting (str "Hello " name)]
    (om/component
      (html [:h1 greeting]))))

(defn start []
  (load! "/api/supervisors")
  (om/root app-state app js/document.body))
