(defproject warden "0.0.1-SNAPSHOT"
  :description "a web app for supervisor"
  :url "https://github.com/eggsby/warden"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/tools.reader "0.8.3"]
                 [compojure "1.1.6"]
                 [clj-yaml "0.4.0"]
                 [necessary-evil "2.0.0"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [om "0.1.7"]
                 [sablono "0.2.1"]
                 [cljs-http "0.1.2"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-cljsbuild "1.0.1"]
            [lein-midje "3.1.3"]]
  :ring {:handler warden.handler/app}
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :cljsbuild {
    :builds [{
        :source-paths ["src/cljs"]
        :test-paths ["test/cljs"]
        :compiler {
          :output-to "resources/public/js/core.js"
          :output-dir "target/cljs-build"
          :optimizations :whitespace
          :pretty-print true}}]}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [midje "1.6.0"]
                        [ring-mock "0.1.5"]]}})
