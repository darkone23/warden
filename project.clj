(defproject warden "0.0.1-SNAPSHOT"
  :description "a web app for supervisor"
  :url "https://github.com/eggsby/warden"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [clj-yaml "0.4.0"]
                 [necessary-evil "2.0.0"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.1.3"]]
  :ring {:handler warden.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [midje "1.6.0"]
                        [ring-mock "0.1.5"]]}})
