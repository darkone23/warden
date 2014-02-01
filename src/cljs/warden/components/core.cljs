(ns warden.components.core
  (:require [warden.net :refer (poll! parse)]
            [warden.components.supervisors :refer (supervisors)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alandipert.storage-atom :refer (local-storage)]
            [cljs.core.async :refer (chan <!)])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(def empty-config {:showing #{}})

(defn gen-conf-reset! [state config]
  "returns a handler for resetting configuration"
  (fn [e]
    (do (reset! config empty-config)
        (om/update! state identity))))

(defn header-menu [state owner]
    (om/component
      (let [reset-handler (gen-conf-reset! state (om/get-state owner :config))]
        (dom/header #js {:className "pure-menu pure-menu-fixed pure-menu-horizontal"}
          (dom/h2 #js {:className "pure-u" :onClick reset-handler} (:name state))
          (dom/h3 #js {:className "description pure-u"} (:description state))))))

(defn app [state owner]
  "App as a function of application state"
  (reify
    om/IRender
    (render [this]
      (let [init {:init-state (om/get-state owner)}]
        (dom/div #js {:className "main pure-g-r"}
          (om/build header-menu state init)
          (om/build supervisors state init))))

    om/IInitState
    (init-state [this]
      {:config (local-storage (atom empty-config) :config)
       :fn identity
       :api-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :api-chan)]
        (poll! "/api/supervisors" 750 ch)
        (go-loop [response (<! ch)]
          (when response
            (let [new-state (parse response)]
              (om/update! state assoc-in [:supervisors] new-state))
            (recur (<! ch))))))))
