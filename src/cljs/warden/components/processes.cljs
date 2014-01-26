(ns warden.components.processes
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn process [{:keys [name statename description]} owner]
  "Single process in a supervisor"
  (om/component
    (dom/li #js {:className (str statename " process pure-u-1")}
      (dom/span #js {:className "name pure-u"} name)
      (dom/span #js {:className "description pure-u"} description)
      (dom/span #js {:className "state pure-u"} statename))))

(defn processes [processes owner]
  "Collection of supervised processes"
  (om/component
    (apply dom/ul #js {:className "processes"}
      (for [{:keys [pid name] :as p} processes]
        (om/build process p
          {:react-key (str pid name)})))))
