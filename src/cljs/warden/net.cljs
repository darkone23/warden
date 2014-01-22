(ns warden.net
  (:require [cljs-http.client :as http]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer [chan >! <! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn poll! [url wait ch]
  "Poll a url every `wait` ms, delivering responses on ch"
  ;; TODO: handle errors from http/get
  (go-loop [enabled true response (<! (http/get url))]
    (>! ch response)
    (<! (timeout wait))
    (recur enabled (<! (http/get url)))))

(defn sync-state! [ch state cursor]
  "Keep cursor synced with http responses from channel"
  (go-loop [response (<! ch)]
    (when response
      (swap! state assoc-in cursor (-> response :body cljson->clj))
      (recur (<! ch)))))
