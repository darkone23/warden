(ns warden.api
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]))

(defn id-for [{:keys [name host]}]
  (str name host))

(def clients
  (into {}
    (for [entry (:hosts config)]
      [(id-for entry) (client entry)])))

(defn supervisors []
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
