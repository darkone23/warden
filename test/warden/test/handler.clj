(ns warden.test.handler
  (:use midje.sweet
        ring.mock.request  
        warden.handler))

(facts "handler routes work as expected"
  (fact "the index route renders the static index.html"
    (let [response (app (request :get "/"))]
      (:status response) => 200
      (:body response) => (-> "public/index.html"
                              clojure.java.io/resource
                              clojure.java.io/file)))

  (fact "the 404 route catches invalid requests"
    (let [response (app (request :get "/invalid"))]
      (:status response) => 404)))
