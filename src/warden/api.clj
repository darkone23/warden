(ns warden.api
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]))

(def clients
  (into {}
    (for [host (:hosts config)]
      [(:name host) (client host)])))

(defn supervisors []
  (for [[name client] clients]
    (merge {:name name} (get-supervisord-info client))))
