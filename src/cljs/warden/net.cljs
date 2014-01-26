(ns warden.net
  (:require [cljs-http.client :as http]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer [>! <! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn parse [response]
  (vec (cljson->clj (:body response))))

(defn poll! [url wait ch]
  "Poll a url every `wait` ms, delivering responses on ch"
  ;; TODO: handle errors from http/get
  (go-loop [enabled true response (<! (http/get url))]
    (>! ch response)
    (<! (timeout wait))
    (recur enabled (<! (http/get url)))))
