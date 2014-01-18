(ns warden.supervisord
  (:require [necessary-evil.core :as xml-rpc]))

(defn xml-url [port] (str "http://" port "/RPC2"))

(defn get-url [{port :port}] (xml-url port))

(defn client [supervisor]
  "Generates a api client out of a config entry"
  (partial xml-rpc/call (get-url supervisor)))

(defn call* [call]
  "Creates a call spec for supervisord"
  (if-not (sequential? call)
    {:methodName (name call)}
    (let [[method & args] call
           method-name (name method)]
      (if args
        {:methodName method-name :params args}
        {:methodName method-name}))))

(defn multi-call [client & calls]
  "Bundle api calls into a single request"
  (client :system.multicall (map call* calls)))

(defn get-supervisord-info [client]
  (let [names   [:version :id :state :pid]
        methods [:supervisor.getSupervisorVersion
                 :supervisor.getIdentification
                 :supervisor.getState
                 :supervisor.getPID]]
    (zipmap names (apply multi-call client methods))))

(defn get-process-info [client name]
  (client :supervisor.getProcessInfo name))

(defn get-all-process-info [client]
  (client :supervisor.getAllProcessInfo))

(defn start [client name]
  (client :supervisor.startProcess name))

(defn start-all [client]
  (client :supervisor.startAllProcesses))

(defn stop [client name]
  (client :supervisor.stopProcess name))

(defn stop-all [client]
  (client :supervisor.stopAllProcesses))

(def api
  {:info       get-supervisord-info
   :status     get-process-info
   :start      start
   :stop       stop
   :stop-all   stop-all
   :start-all  start-all
   :status-all get-all-process-info})

(def methods*
  "Exhaustive list of supervisord API, for reference"
  ["supervisor.addProcessGroup"
   "supervisor.clearAllProcessLogs"
   "supervisor.clearLog"
   "supervisor.clearProcessLog"
   "supervisor.clearProcessLogs"
   "supervisor.getAPIVersion"
   "supervisor.getAllConfigInfo"
   "supervisor.getAllProcessInfo"
   "supervisor.getIdentification"
   "supervisor.getPID"
   "supervisor.getProcessInfo"
   "supervisor.getState"
   "supervisor.getSupervisorVersion"
   "supervisor.getVersion"
   "supervisor.readLog"
   "supervisor.readMainLog"
   "supervisor.readProcessLog"
   "supervisor.readProcessStderrLog"
   "supervisor.readProcessStdoutLog"
   "supervisor.reloadConfig"
   "supervisor.removeProcessGroup"
   "supervisor.restart"
   "supervisor.sendProcessStdin"
   "supervisor.sendRemoteCommEvent"
   "supervisor.shutdown"
   "supervisor.startAllProcesses"
   "supervisor.startProcess"
   "supervisor.startProcessGroup"
   "supervisor.stopAllProcesses"
   "supervisor.stopProcess"
   "supervisor.stopProcessGroup"
   "supervisor.tailProcessLog"
   "supervisor.tailProcessStderrLog"
   "supervisor.tailProcessStdoutLog"
   "system.listMethods"
   "system.methodHelp"
   "system.methodSignature"
   "system.multicall"])
