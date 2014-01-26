(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]
            [liberator.core :refer (defresource resource)]
            [clojure.data.json :as json]
            [tailrecursion.cljson :refer (clj->cljson)]
            [clojure.core.match :refer (match)]))

(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(def supervisor-clients
  ;; TODO: push down, this should probably be the
  ;; normal serverside representation of a supervisor
  (for [supervisor (:hosts config)]
    (assoc supervisor
      :client (client supervisor)
      :id (supervisor-id supervisor))))

(defn get-supervisors [supervisors]
  (map deref
    (for [{:keys [client host name port]} supervisors]
      (future
        (merge {:host host :port port :name name}
               (get-supervisord-info client))))))

(defn gen-supervisors-resource [supervisors]
  (resource
    :available-media-types ["application/json" "application/edn" "application/cljson"]
    :allowed-methods [:get]
    :exists? (fn [_] (pos? (count supervisors)))
    :handle-ok
      (fn [r]
        (let [media-type (get-in r [:representation :media-type])]
          (match [media-type]
            ["application/json"]   (json/write-str (get-supervisors supervisors))
            ["application/edn"]    (pr-str         (get-supervisors supervisors))
            ["application/cljson"] (clj->cljson    (get-supervisors supervisors))
            :else nil)))))

(defroutes api-routes
  (ANY "/supervisors" []
    (gen-supervisors-resource supervisor-clients)))
