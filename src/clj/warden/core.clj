(ns warden.core
  (:gen-class)
  (:require [warden.handler :refer [app]]
            [org.httpkit.server :refer [run-server]]))

(defn -main [& args]
  (let [env-port (System/getenv "PORT")
        port (if env-port (Integer/parseInt env-port) 8080)]
    (println (str "Starting server on http://0.0.0.0:" port))
    (run-server app {:port port})))
           
