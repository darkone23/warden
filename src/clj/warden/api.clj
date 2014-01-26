(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]
            [liberator.core :refer (defresource)]
            [clojure.data.json :as json]
            [tailrecursion.cljson :refer (clj->cljson)]
            [clojure.core.match :refer (match)]))

(defn id-for [{:keys [name host]}]
  (str name host))

(def clients
  (into {}
    (for [entry (:hosts config)]
      [(id-for entry) (client entry)])))

(defn get-supervisors []
  "Fetch information about all supervisors in the config...
   now with concurrency!"
  (map deref
    (for [{:keys [host name port] :as entry} (:hosts config)]
      (future
        (let [client (get clients (id-for entry))]
          (merge {:host host
                  :port port
                  :name name}
                 (get-supervisord-info client)))))))

(defresource supervisors-resource []
  :available-media-types ["application/json" "application/edn" "application/cljson"]
  :allowed-methods [:get]
  :handle-ok
    (fn [r]
      (let [media-type (get-in r [:representation :media-type])]
        (match [media-type]
          ["application/json"] (json/write-str (supervisors))
          ["application/edn"] (pr-str (supervisors))
          ["application/cljson"] (clj->cljson (supervisors))
          :else nil))))

(defroutes api-routes
  (ANY "/supervisors" [] (supervisors-resource)))

