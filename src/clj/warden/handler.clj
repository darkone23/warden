(ns warden.handler
  (:use compojure.core)
  (:require [warden.api :refer (api-routes)]
            [ring.util.response :as resp]
            [ring.middleware.gzip :as gzip]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (context "/api" [] api-routes)
  (route/resources "/")
  (route/not-found "404 Not Found"))

(def app
  (-> app-routes
      handler/site
      gzip/wrap-gzip))
