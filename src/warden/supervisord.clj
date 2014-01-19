(ns warden.supervisord
  (:require [necessary-evil.core :as xml-rpc]))

(defn xml-url [port] (str "http://" port "/RPC2"))

(defn get-url [{port :port}] (xml-url port))

(defn client [supervisor]
  "Generates an api client out of a config entry"
  (partial xml-rpc/call (get-url supervisor)))

(defn- call* [call]
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
  "Fetches invormation about the supervisord server"
  (let [names   [:version :id :state :pid]
        methods [:supervisor.getSupervisorVersion
                 :supervisor.getIdentification
                 :supervisor.getState
                 :supervisor.getPID]]
    (zipmap names (apply multi-call client methods))))

(defn get-process-info [client name]
   "Returns a map of information about a process"
  (client :supervisor.getProcessInfo name))

(defn get-all-process-info [client]
  "Reads all process information from a supervisord"
  (client :supervisor.getAllProcessInfo))

(defn start [client name]
  "Starts a supervised process"
  (client :supervisor.startProcess name))

(defn start-all [client]
  "Starts all supervised process"
  (client :supervisor.startAllProcesses))

(defn stop [client name]
  "Stops a supervised process"
  (client :supervisor.stopProcess name))

(defn stop-all [client]
  "Stops all supervised process"
  (client :supervisor.stopAllProcesses))

(defn read-supervisord-log [client offset bytes]
  "Reads a number of bytes from the supervisord log, starting at offset"
  (client :supervisor.readLog offset bytes))

(defn read-full-supervisord-log [client]
  "Reads the entire supervisord log"
  (read-supervisord-log client 0 0))

(defn read-process-stdout [client name offset bytes]
  "Reads a number of bytes from the stdout of a process, starting at offset"
  (client :supervisor.readProcessStdoutLog name offset bytes))

(defn read-full-process-stdout [client name]
  "Reads the full stdout from a supervised process"
  (read-process-stdout client name 0 0))

(defn read-process-stderr [client name offset bytes]
  "Reads a number of bytes from the stderr of a process, starting at offset"
  (client :supervisor.readProcessStderrLog name offset bytes))

(defn read-full-process-stderr [client name]
  "Reads the full stderr from a supervised process"
  (read-process-stderr client name 0 0))

(defn tail-process-stdout [client name offset bytes]
  "Reads some number of bytes from stdout of a process
   Starts at offset, or end of file if offset+bytes doesn't reach EOF
   returns [content new-offset overflow?]"
  (client :supervisor.readProcessStdoutLog name offset bytes))

(defn tail-process-stderr [client name offset bytes]
  "Reads some number of bytes from stderr of a process
   Starts at offset, or end of file if offset+bytes doesn't reach EOF
   returns [content new-offset overflow?]"
  (client :supervisor.readProcessStdoutLog name offset bytes))

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
