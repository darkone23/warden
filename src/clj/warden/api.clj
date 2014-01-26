(ns warden.api
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]
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

(defn safe-parse-int [s]
  (try (Integer/parseInt s)
    (catch Exception e)))

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
  "Collection of supervisors resource"
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

(defn gen-supervisor-resource [supervisors]
  "Single supervisor resource"
  (resource
    :available-media-types ["application/json" "application/edn" "application/cljson"]
    :allowed-methods [:get]
    :exists? (fn [_] (= 1 (count supervisors)))
    :handle-ok
      (fn [r]
        (let [media-type (get-in r [:representation :media-type])]
          (match [media-type]
            ["application/json"]   (json/write-str (first (get-supervisors supervisors)))
            ["application/edn"]    (pr-str         (first (get-supervisors supervisors)))
            ["application/cljson"] (clj->cljson    (first (get-supervisors supervisors)))
            :else nil)))))

(defroutes api-routes
  (ANY "/supervisors" []
    (gen-supervisors-resource supervisor-clients))
  (ANY "/supervisors/:host" [host]
    (gen-supervisors-resource
      (filter-key= {:host host} supervisor-clients)))
  (ANY "/supervisors/:host/:port" [host port]
    (gen-supervisor-resource
      (filter-key= {:host host :port (safe-parse-int port)} supervisor-clients))))
