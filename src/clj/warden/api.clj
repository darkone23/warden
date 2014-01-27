(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info api)]
            [liberator.core :refer (defresource resource)]
            [clojure.data.json :as json]
            [tailrecursion.cljson :refer (clj->cljson)]
            [clojure.core.match :refer (match)]))

;; helper fns
(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn key= [x y]
  "Checks that every key in x is equal in y"
  (every? true? (for [[k v] x] (= v (get y k)))))

(defn filter-key= [comparison ms]
  (filter #(key= comparison %) ms))

(defn find-key= [comparison ms]
  (first (filter-key= comparison ms)))

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


;; Liberator Resource Definitions

(defn gen-supervisors-resource [supervisors]
  "Collection of supervisors resource"
  (resource
    :available-media-types ["application/json" "application/edn" "application/cljson"]
    :allowed-methods [:get]
    :exists? (fn [_] (pos? (count supervisors)))
    :handle-ok
      (fn [r]
        (let [media-type (get-in r [:representation :media-type])]
          (match [media-type]
            ["application/json"]
              (json/write-str (get-supervisors supervisors))
            ["application/edn"]
              (pr-str (get-supervisors supervisors))
            ["application/cljson"]
              (clj->cljson (get-supervisors supervisors)))))))

(defn gen-supervisor-resource [supervisor]
  "Single supervisor resource"
  (resource
    :available-media-types ["application/json" "application/edn" "application/cljson"]
    :allowed-methods [:get]
    :exists? (fn [_] (:client supervisor))
    :handle-ok
      (fn [r]
        (let [media-type (get-in r [:representation :media-type])]
          (match [media-type]
            ["application/json"]
              (json/write-str (-> supervisor list get-supervisors first))
            ["application/edn"]
              (pr-str (-> supervisor list get-supervisors first))
            ["application/cljson"]
              (clj->cljson (-> supervisor list get-supervisors first)))))))

(defn supervisors-list []
  (gen-supervisors-resource supervisor-clients))

(defn supervisors-list-by-host [host]
  (gen-supervisors-resource
    (filter-key= {:host host} supervisor-clients)))

(defn supervisor-by-id [host name]
  (gen-supervisor-resource
    (find-key= {:host host :name name} supervisor-clients)))

(defn supervisor-process-list [host name])
(defn supervisor-process-detail [host name process])

(defn supervisor-process-action [host name process action]
  (let [action (get api (keyword action))
        {client :client} (find-key= {:host host :name name} supervisor-clients)]
    (if (and action client)
      (resource
        :allowed-methods [:post]
        :available-media-types ["application/json" "application/edn" "application/cljson"]
        :post! (fn [_] {::result (action client process)})
        :handle-created
          (fn [r]
            (let [result (::result r)
                  media-type (get-in r [:representation :media-type])]
              (match [media-type]
                ["application/json"]   (json/write-str result)
                ["application/edn"]    (pr-str result)
                ["application/cljson"] (clj->cljson result))))))))

;; Compojure Route Definitions

(defroutes api-routes
  (ANY "/supervisors" []
    (supervisors-list))
  (ANY "/supervisors/:host" [host]
    (supervisors-list-by-host host))
  (ANY "/supervisors/:host/:name" [host name]
    (supervisor-by-id host name))
  (ANY "/supervisors/:host/:name/processes" [host name]
    (supervisor-process-list [host name]))
  (ANY "/supervisors/:host/:name/processes/:process" [host name process]
    (supervisor-process-detail host name process))
  (ANY "/supervisors/:host/:name/processes/:process/action/:action" [host name process action]
    (supervisor-process-action host name process action)))
