(ns warden.supervisord
  (:require [necessary-evil.core :as xml-rpc]
            [clojure.core.async :refer (chan timeout close! alts! go-loop >!)]))

(defn xml-url [port] (str "http://" port "/RPC2"))

(defn get-url [{port :port}] (xml-url port))

(defn client [supervisor]
  "Generates an api client out of a config entry"
  (partial xml-rpc/call (get-url supervisor)))

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

(defn get-supervisord-version [client]
  (client :supervisor.getSupervisorVersion))

(defn get-supervisord-id [client]
  (client :supervisor.getIdentification))

(defn get-supervisord-state [client]
  (client :supervisor.getState))

(defn get-supervisord-pid [client]
  (client :supervisor.getPID))

(defn get-supervisord-info [client]
  "Fetches invormation about the supervisord server"
  (let [processes (future (get-all-process-info    client))
        version   (future (get-supervisord-version client))
        id        (future (get-supervisord-id      client))
        state     (future (get-supervisord-state   client))
        pid       (future (get-supervisord-pid     client))]
    {:processes @processes
     :version   @version
     :id        @id
     :state     @state
     :pid       @pid}))

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
  (client :supervisor.tailProcessStdoutLog name offset bytes))

(defn tail-process-stderr [client name offset bytes]
  "Reads some number of bytes from stderr of a process
   Starts at offset, or end of file if offset+bytes doesn't reach EOF
   returns [content new-offset overflow?]"
  (client :supervisor.tailProcessStderrLog name offset bytes))

(defn- tail-channel* [tail-fn client name]
  "Creates a channel which will emit lines from the remote clients process
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (let [log-chan (chan 100) control-chan (chan 1)
        wait 1000 chunk-size 5000]
    (go-loop [[content offset _] (tail-fn client name 0 chunk-size)]
      (if (empty? content)
        ;; no lines for reading, start over
        (let [[msg ch] (alts! [control-chan (timeout wait)])]
          (if (= control-chan ch)
            (map close! [log-chan control-chan])
            (recur (tail-fn client name offset chunk-size))))
        ;; process lines, adjust offset, start over
        (let [num-lines (count (re-seq #"\n" content))
              lines     (take num-lines (re-seq #"[^\n]+" content))
              read-size (reduce + num-lines (map count lines))
              offset    (- offset (- (count content) read-size))]
          (doseq [line lines] (>! log-chan line))
          (let [[msg ch] (alts! [control-chan (timeout wait)])]
            (if (= control-chan ch)
              (map close! [log-chan control-chan])
              (recur (tail-fn client name offset chunk-size)))))))
    [log-chan control-chan]))

(defn process-stdout-chan [client name]
  "Creates a channel which will produce lines from the remote process stdout
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (tail-channel* tail-process-stdout client name))

(defn process-stderr-chan [client name]
  "Creates a channel which will produce lines from the remote process stderr
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (tail-channel* tail-process-stderr client name))

(def api
  {:info        get-supervisord-info
   :status      get-process-info
   :start       start
   :stop        stop
   :stop-all    stop-all
   :start-all   start-all
   :status-all  get-all-process-info
   :read-log    read-full-supervisord-log
   :read-stdout read-full-process-stdout
   :read-stderr read-full-process-stderr
   :tail-stdout tail-process-stdout
   :tail-stderr tail-process-stderr})

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
