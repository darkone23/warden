(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :as super]
            [liberator.core :refer (defresource resource)]
            [liberator.representation :refer (render-map-generic render-seq-generic)]
            [tailrecursion.cljson :refer (clj->cljson)]
            [clojure.core.match :refer (match)]))

;; helper fns
(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn key= [x y]
  "Checks that every key in x is equal in y"
  (if (every? true? (for [[k v] x] (= v (get y k)))) y))

(defn filter-key= [m ms]
  "filters a collection of maps by those that match a minimum keyset"
  (filter (partial key= m) ms))

(defn some-key= [m ms]
  "felect by minimum keyset"
  (some (partial key= m) ms))

;; Liberator Resource Definitions
(defmethod render-map-generic
  "application/cljson"
  [m ctx] (clj->cljson m))

(defmethod render-seq-generic
  "application/cljson"
  [s ctx] (clj->cljson s))

(def supervisor-clients
  ;; what is this global data doing here????
  (super/supervisor-clients (:hosts config)))

(defresource supervisors-all []
  :available-media-types ["application/json""application/edn" "application/cljson"]
  :allowed-methods [:get]
  :handle-ok (fn [ctx] (super/get-supervisors supervisor-clients)))

(defresource supervisors-group [host]
  :available-media-types ["application/json" "application/edn" "application/cljson"]
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (let [cs (filter-key= {:host host} supervisor-clients)]
               (if-let [s (super/get-supervisors cs)] {::supervisors s})))
  :handle-ok ::supervisors)

(defresource supervisor [host name]
  :available-media-types ["application/json""application/edn" "application/cljson"]
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (let [c (some-key= {:host host :name name} supervisor-clients)]
               (if-let [s (super/get-supervisor c)] {::supervisor s})))
  :handle-ok ::supervisor)

(defresource supervisor-processes [host name])
(defresource supervisor-process [host name process])

(defresource supervisor-process-action [host name process action]
  :allowed-methods [:post]
  :available-media-types ["application/json" "application/edn" "application/cljson"]
  :post! (fn [ctx]
           (let [c (:client (some-key= {:host host :name name} supervisor-clients))
                 f (super/api (keyword action))]
             (println "hi" f c)
             (if (and c f) {::result (f c process)})))
  :handle-created ::result)

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
