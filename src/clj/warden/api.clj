(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :as super]
            [warden.util :refer (supervisor-id filter-key= some-key=)]
            [liberator.core :refer (defresource resource)]
            [liberator.representation :refer (render-map-generic render-seq-generic)]
            [tailrecursion.cljson :refer (clj->cljson)]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows :as workflows]
                             [credentials :as creds]]))

;; helper fns

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

(defresource supervisor-process [host name process]
  :available-media-types media-types
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (if-let [s (some-key= {:host host :name name} (read-supervisors))]
               (if-let [p (some-key= {:name process} (:processes s))]
                 {::response p})))
  :handle-ok ::response)

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

(defn supervisor-process-log-out [host name process]
  (println :out host name process))

(defn supervisor-process-log-err [host name process]
  (println :err host name process))

(defroutes api-routes*
  (ANY "/supervisors" []
    (supervisors-all))
  (ANY "/supervisors/:host" [host]
    (supervisors-group host))
  (ANY "/supervisors/:host/:name" [host name]
    (supervisor host name))
  (ANY "/supervisors/:host/:name/processes/:process" [host name process]
    (supervisor-process host name process))
  (ANY "/supervisors/:host/:name/processes/:process/log/out" [host name process]
    (supervisor-process-log-out host name process))
  (ANY "/supervisors/:host/:name/processes/:process/log/err" [host name process]
    (supervisor-process-log-err host name process))
  (ANY "/supervisors/:host/:name/processes/:process/action/:action" [host name process action]
    (supervisor-process-action host name process action)))

(defn authorize* [routes user pass]
  (let [auth-name "secured warden"
        credentials {user {:username user
                           :password (creds/hash-bcrypt pass)}}
        unauthed (fn [req] (workflows/http-basic-deny auth-name req))
        auth     (fn [req] (creds/bcrypt-credential-fn credentials req))
        configuration {:allow-anon? false
                       :unauthenticated-handler unauthed
                       :workflows [(workflows/http-basic
                                     :credential-fn auth
                                     :realm auth-name)]}]
    (friend/authenticate routes configuration)))

(defn authorize [routes {:keys [user pass]}]
  (if (and user pass)
    (authorize* routes user pass)
    routes))

(def api-routes (authorize api-routes* config))
