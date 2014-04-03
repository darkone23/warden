(ns warden.schemas
         (:require [schema.core :as s])
  #+cljs (:require-macros [schema.macros :as s]))

(def SupervisorHost
  {:name s/Str
   :host s/Str
   :port s/Num
   (s/optional-key :user) s/Str
   (s/optional-key :pass) s/Str})

(def Configuration
  {:hosts [SupervisorHost]
   :interval s/Num
   (s/optional-key :user) s/Str
   (s/optional-key :pass) s/Str})

(def SupervisorProcess
  {:group          s/Str
   :name           s/Str
   :description    s/Str
   :statename      s/Str
   :spawnerr       s/Str
   :logfile        s/Str
   :stdout_logfile s/Str
   :stderr_logfile s/Str
   :pid            s/Num
   :state          s/Num
   :exitstatus     s/Num
   :start          s/Num
   :stop           s/Num
   :now            s/Num})

(def SupervisorProcessStatus
  {:name        s/Str
   :group       s/Str
   :description s/Str
   :status      s/Num})

(def SupervisordState
  {:statecode (s/enum 2 1 0 -1)
   :statename (s/enum "FATAL"
                      "RUNNING"
                      "RESTARTING"
                      "SHUTDOWN")})

(def Fault
  {:fault-code   s/Num
   :fault-string s/Str})

(defn maybe-err [x] (s/either x Fault))

(def SupervisordInfo
  {:processes [SupervisorProcess]
   :state     SupervisordState})
