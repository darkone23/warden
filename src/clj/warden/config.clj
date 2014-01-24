(ns warden.config
  (:require [warden.schemas :refer (Configuration)]
            [schema.core :refer (validate)]
            [clj-yaml.core :as yaml]))

(defn valid-config [config]
  "returns a valid configuration or nothing"
  (try
    (validate Configuration config)
    (catch Exception e
      (println (.getMessage e)))))

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
    (or (valid-config config)
        (do (println "Malformed config: continuing without configuration...")
            {:hosts []}))))
