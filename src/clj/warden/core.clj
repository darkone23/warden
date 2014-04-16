(ns warden.core
  (:gen-class)
  (:require [warden.handler :refer [app]]
            [org.httpkit.server :refer [run-server]]))

(defn -main [& args]
  (let [port 8080]
    (println (str "Starting server on http://0.0.0.0:" port))
    (run-server app {:port port})))
           
