(ns warden.handler
  (:use compojure.core)
  (:require [warden.config :refer (config)]
            [ring.util.response :as resp]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "404 Not Found"))

(def app
  (handler/site app-routes))
