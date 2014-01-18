(ns warden.handler
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "404 Not Found"))

(def app
  (handler/site app-routes))
