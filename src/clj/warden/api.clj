(ns warden.api
  (:use compojure.core)
  (:require [clojure.string :as str]
            [warden.config :refer (config)]
            [warden.supervisord :as super]
            [warden.util :refer (supervisor-id filter-key= some-key=)]
            [liberator.core :refer (defresource resource)]
            [liberator.representation :refer (render-map-generic render-seq-generic)]
            [tailrecursion.cljson :refer (clj->cljson)]
            [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! put! close! go-loop timeout alts! chan]]
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

(defn get-supervisor-process [host name process]
  (if-let [s (some-key= {:host host :name name} (read-supervisors))]
    (some-key= {:name process} (:processes s))))

(defresource supervisor-process [host name process]
  :available-media-types media-types
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (if-let [p (get-supervisor-process host name process)]
               {::response p}))
  :handle-ok ::response)

(defn get-supervisor-client [host name]
  (:client (some-key= {:host host :name name} supervisor-clients)))

(defresource supervisor-process-action [host name process action]
  :available-media-types media-types
  :allowed-methods [:post]
  :exists? (fn [ctx]
             (let [c (get-supervisor-client host name)
                   f (super/api (keyword action))]
               (if (and c f) {::client c ::action f})))
  :post-to-missing? false
  :post! (fn [ctx]
           (let [client (::client ctx) action (::action ctx)]
             {::response {:result (action client process)}}))
  :handle-created ::response)

(defn process-log-ws [log-chan-fn {{:keys [host name process]} :params :as req}]
  (if (get-supervisor-process host name process)
    (let [client (get-supervisor-client host name)
          [log-ch control-ch] (log-chan-fn client process)]
      (with-channel req ws-ch
        (go-loop [[msg ch] (alts! [ws-ch log-ch])]
          (if (= ch ws-ch)
            (doseq [c [control-ch ws-ch]] (close! c))
            (let [msg (str/replace msg #"\t" "    ")]
              (doseq [line (str/split msg #"\n")] (put! ws-ch line))
              (recur (alts! [ws-ch log-ch])))))))
    {:status 404}))

(defn supervisor-process-log-out [req]
  (process-log-ws (:stdout-chan super/api) req))

(defn supervisor-process-log-err [req]
  (process-log-ws (:stderr-chan super/api) req))

(defroutes api-routes*
  (ANY "/supervisors" []
    (supervisors-all))
  (ANY "/supervisors/:host" [host]
    (supervisors-group host))
  (ANY "/supervisors/:host/:name" [host name]
    (supervisor host name))
  (ANY "/supervisors/:host/:name/processes/:process" [host name process]
    (supervisor-process host name process))
  (GET "/supervisors/:host/:name/processes/:process/log/out" req
    (supervisor-process-log-out req))
  (GET "/supervisors/:host/:name/processes/:process/log/err" req
    (supervisor-process-log-err req))
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
