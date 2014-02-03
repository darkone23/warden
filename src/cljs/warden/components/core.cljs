(ns warden.components.core
  (:require [warden.net :refer (poll! parse)]
            [warden.components.supervisors :refer (supervisors)]
            [warden.components.processes :refer (processes)]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alandipert.storage-atom :refer (local-storage)]
            [cljs.core.async :refer (chan <!)])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def empty-config {:showing #{}})

(defn header-menu [{:keys [route description] :as state} owner]
  (om/component
   (dom/header #js {:className "pure-menu pure-menu-fixed pure-menu-open pure-menu-horizontal"}
     (dom/h2 #js {:className "pure-u"} (:name state))
     (dom/h3 #js {:className "description pure-u"} description)
     (dom/ul #js {:className (str "pure-u " (name route))}
       (dom/li #js {:key "nav-supervisors" :className "pure-u supervisors"}
         (dom/a #js {:href "#/supervisors"} "supervisors")
       (dom/li #js {:key "nav-processes" :className "pure-u processes"}
         (dom/a #js {:href "#/processes"} "processes")))))))

(defn app [state owner]
  "App as a function of application state"
  (reify
    om/IRender
    (render [this]
      (let [init {:init-state (om/get-state owner)}
            route (:route state)]
        (dom/div #js {:className "main pure-g-r"}
          (om/build header-menu state init)
          (case (:route state)
            :supervisors (om/build supervisors state init)
            :processes (om/build processes state init)))))

    om/IInitState
    (init-state [this]
      {:config (local-storage (atom empty-config) :config)
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
