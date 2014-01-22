(ns warden.config
  (:require [clj-yaml.core :as yaml]))

(defn valid-entry? [entry]
  "Verify a host entry provides a name/port pair"
  (and (map? entry)
       (= #{:name :host :port}
          (-> entry keys set))))

(defn valid-config? [config]
  "Ensures configuration structure"
  (if-let [{hosts :hosts} config]
    (and (sequential? hosts)
         (every? valid-entry? hosts))))

(defn read-config []
  "Reads the config from the the 'config' system property"
  (try (slurp (System/getProperty "config" "warden.yaml"))
    (catch Exception e
      (do (println "Could not load config: " (.getMessage e))
          (println "Continuing without configuration...")
          "hosts: []"))))

(def config
  "Warden application configuration"
  (let [config (yaml/parse-string (read-config))]
    (if (valid-config? config)
      config
      (do (println "Malformed config: continuing without configuration...")
          {:hosts []}))))
