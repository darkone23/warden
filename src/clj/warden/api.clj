(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :as super]
            [liberator.core :refer (defresource resource)]
            [liberator.representation :refer (render-map-generic render-seq-generic)]
            [tailrecursion.cljson :refer (clj->cljson)]))

;; helper fns
(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn key= [x y]
  "Checks that every key in x is equal in y"
  (when (every? true? (for [[k v] x] (= v (get y k)))) y))

(defn filter-key= [m ms]
  "filters a collection of maps by those that match a minimum keyset"
  (filter (partial key= m) ms))

(defn some-key= [m ms]
  "select by minimum keyset"
  (some (partial key= m) ms))

;; GLOBALS WHAT ARE THESE DOING HERE????
(def supervisor-clients (super/supervisor-clients (:hosts config)))
(def supervisors-atom (super/sync-supervisors! supervisor-clients (:interval config)))
(defn read-supervisors [] (get @supervisors-atom :supervisors))

(def last-modified (atom nil))
(add-watch supervisors-atom :last-modified
  (fn [_ _ _ _] (reset! last-modified (new java.util.Date))))

(defn modified [_] @last-modified)
(defn etag [{r ::response}] (hash r))

(def media-types ["application/json" "application/edn" "application/cljson"])

;; Liberator Resource Definitions
(defmethod render-map-generic
  "application/cljson"
  [m ctx] (clj->cljson m))

(defmethod render-seq-generic
  "application/cljson"
  [s ctx] (clj->cljson s))

(defresource supervisors-all []
  :available-media-types media-types
  :allowed-methods [:get]
  :etag etag
  :last-modified modified
  :exists? (fn [ctx]
             (let [s (read-supervisors)]
               (when (seq s) {::response s})))
  :handle-ok ::response)

(defresource supervisors-group [host]
  :available-media-types media-types
  :allowed-methods [:get]
  :etag etag
  :last-modified modified
  :exists? (fn [ctx]
             (let [s (filter-key= {:host host} (read-supervisors))]
               (when (seq s) {::response s})))
  :handle-ok ::response)

(defresource supervisor [host name]
  :available-media-types media-types
  :allowed-methods [:get]
  :etag etag
  :last-modified modified
  :exists? (fn [ctx]
             (if-let [s (some-key= {:host host :name name} (read-supervisors))]
               {::response s}))
  :handle-ok ::response)

(defresource supervisor-processes [host name])
(defresource supervisor-process [host name process])

(defresource supervisor-process-action [host name process action]
  :available-media-types media-types
  :allowed-methods [:post]
  :exists? (fn [ctx]
             (let [c (:client (some-key= {:host host :name name} supervisor-clients))
                   f (super/api (keyword action))]
               (if (and c f) {::client c ::action f})))
  :post-to-missing? false
  :post! (fn [ctx]
           (let [client (::client ctx) action (::action ctx)]
             {::response {:result (action client process)}}))
  :handle-created ::response)

;; Compojure Route Definitions
(defroutes api-routes
  ;; list of all supervisors
  (ANY "/supervisors" []
    (supervisors-all))
  ;; list of supervisors on a host
  (ANY "/supervisors/:host" [host]
    (supervisors-group host))
  ;; a particular supervisor on a host
  (ANY "/supervisors/:host/:name" [host name]
    (supervisor host name))
  ;; a process list of a supervisor on a host
  (ANY "/supervisors/:host/:name/processes" [host name]
    (supervisor-processes host name))
  ;; detail about a particular process
  (ANY "/supervisors/:host/:name/processes/:process" [host name process]
    (supervisor-process host name process))
  ;; action enacted on a particular process
  (ANY "/supervisors/:host/:name/processes/:process/action/:action" [host name process action]
    (supervisor-process-action host name process action)))
