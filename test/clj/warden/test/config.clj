(ns warden.test.config
  (:require [clj-yaml.core :as yaml])
  (:use midje.sweet
        warden.config))

(facts "warden can take configuration"
  (fact "the example config is a valid config"
    (let [config (-> "example.warden.yaml" slurp yaml/parse-string)]
      (valid-config config) => config)))
