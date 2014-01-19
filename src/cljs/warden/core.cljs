(ns warden.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer [html] :include-macros true]))

(def state {:name "warden"})

(defn app [state]
  (let [{name :name} state
        greeting (str "Hello " name "!")]
    (om/component
      (html [:h1 greeting]))))

(defn start []
  (om/root state app js/document.body))
