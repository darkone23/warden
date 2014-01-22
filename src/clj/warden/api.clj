(ns warden.api
  (:require [warden.config :refer (config)]
            [warden.supervisord :refer (client get-supervisord-info)]))

(def clients
  (into {}
    (for [host (:hosts config)]
      [(:name host) (client host)])))

(defn supervisors []
  "Fetch information about all supervisors in the config...
   now with concurrency!"
  (map deref
    (for [{:keys [host name port]} (:hosts config)]
      (future
        (let [client (get clients name)]
          (merge {:host host :port port :name name}
                 (get-supervisord-info client)))))))
