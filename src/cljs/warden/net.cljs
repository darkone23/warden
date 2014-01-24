(ns warden.net
  (:require [om.core :as om :include-macros true]
            [cljs-http.client :as http]
            [tailrecursion.cljson :refer [cljson->clj]]
            [cljs.core.async :refer [chan >! <! timeout]])
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

(defn sync-state! [ch owner ks]
  "Keep cursor synced with http responses from channel"
  (go-loop [response (<! ch)]
    (when response
      (let [new-state (parse response)]
        (om/update! (om/get-props owner) assoc-in ks new-state))
      (recur (<! ch)))))
