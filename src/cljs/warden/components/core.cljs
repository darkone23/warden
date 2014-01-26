(ns warden.components.core
  (:require [warden.net :refer (poll! sync-state!)]
            [warden.components.supervisors :refer (supervisors)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alandipert.storage-atom :refer (local-storage)]
            [cljs.core.async :refer (chan)]))

(defn header-menu [state owner]
  (om/component
   (dom/header #js {:className "pure-menu pure-menu-fixed pure-menu-horizontal"}
    (dom/h2 #js {:className "pure-u"} (:name state))
    (dom/h3 #js {:className "description pure-u"} (:description state)))))

(defn app [state owner]
  "App as a function of application state"
  (reify
    om/IInitState
    (init-state [this]
      {:config (local-storage (atom {:showing #{}}) :config)
       :api-chan (chan 1)})

    om/IWillMount
    (will-mount [this]
      (let [ch (om/get-state owner :api-chan)]
        (poll! "/api/supervisors" 2500 ch)
        (sync-state! ch owner [:supervisors])))

    om/IRender
    (render [this]
      (dom/div #js {:className "main pure-g-r"}
        (om/build header-menu state)
        (om/build supervisors state
          {:init-state (om/get-state owner)})))))
