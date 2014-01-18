(ns warden.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (System/getProperty "config"))
  (route/resources "/")
  (route/not-found "404 Not Found"))

(def app
  (handler/site app-routes))
