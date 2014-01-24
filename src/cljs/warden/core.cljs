(ns warden.core
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer (<!)]
            [cljs-http.client :as http]
            [alandipert.storage-atom :refer (local-storage)]
            [warden.components :refer (app)]
            [warden.net :refer (parse)])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn ^:export start []
  (go
   (let [supervisors (parse (<! (http/get "/api/supervisors")))
         config (local-storage (atom {:showing #{}}))
         app-state (atom {:name "warden"
                          :description "process management"
                          :supervisors supervisors
                          :config config})]
    (om/root app-state app js/document.body))))
