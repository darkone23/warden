(ns warden.handler
  (:use compojure.core)
  (:require [warden.api :refer (supervisors)]
            [ring.util.response :as resp]
            [ring.middleware.gzip :as gzip]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (route/resources "/")
  (GET "/api/supervisors" [] (prn-str (supervisors)))
  (route/not-found "404 Not Found"))

(def app
  (-> app-routes
      handler/site
      gzip/wrap-gzip))
