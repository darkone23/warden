(ns warden.supervisord
  (:require [warden.schemas :refer (SupervisorProcess SupervisorProcessStatus SupervisordState SupervisordInfo maybe-err)]
            [necessary-evil.core :as xml-rpc]
            [schema.core :as s]
            [clojure.core.async :refer (chan timeout close! alts! go-loop >! <! thread)]))

(defn xml-url [host port] (str "http://" host ":" port "/RPC2"))

(defn get-url [{:keys [host port user pass]}]
  (let [host (if (and user pass)
               (str user ":" pass "@" host)
               host)]
    (xml-url host port)))

(defn client [supervisor]
  "Generates an api client out of a config entry"
  (let [url (get-url supervisor)]
    (fn [& args]
      (try
        (let [{:keys [fault-code fault-string] :as r} (apply xml-rpc/call url args)]
          (if-not fault-code r {:fault-code fault-code :fault-string fault-string}))
        (catch Exception e
          {:fault-code -1
           :fault-string (.getMessage e)})))))

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

(s/defn get-process-info :- (maybe-err SupervisorProcess)
  [client name]
  "Returns a map of information about a process"
  (client :supervisor.getProcessInfo name))

(s/defn get-all-process-info :- (maybe-err [SupervisorProcess])
  [client]
  "Reads all process information from a supervisord"
  (client :supervisor.getAllProcessInfo))

(s/defn start :- (maybe-err Boolean)
  [client name]
  "Starts a supervised process"
  (client :supervisor.startProcess name))

(s/defn start-all [client] :- (maybe-err [SupervisorProcessStatus])
  "Starts all supervised process"
  (client :supervisor.startAllProcesses))

(s/defn stop [client name] :- (maybe-err Boolean)
  "Stops a supervised process"
  (client :supervisor.stopProcess name))

(s/defn stop-all [client] :- (maybe-err [SupervisorProcessStatus])
  "Stops all supervised process"
  (client :supervisor.stopAllProcesses))

(s/defn restart [client name] :- (maybe-err Boolean)
  "Stops a supervised process, returns whether it started properly"
  (second
    (multi-call client
      [:supervisor.stopProcess name]
      [:supervisor.startProcess name])))

(s/defn get-supervisord-version :- (maybe-err s/Str)
  [client]
  (client :supervisor.getSupervisorVersion))

(s/defn get-supervisord-id :- (maybe-err s/Str)
  [client]
  (client :supervisor.getIdentification))

(s/defn get-supervisord-state :- (maybe-err SupervisordState)
  [client]
  (client :supervisor.getState))

(s/defn get-supervisord-pid :- (maybe-err s/Num)
  [client]
  (client :supervisor.getPID))

(defn- map-vals [f m]
  (reduce (fn [m [k v]] (assoc m k (f v))) {} m))

(s/defn get-supervisord-info :- (map-vals maybe-err SupervisordInfo)
  [client]
  "Fetches invormation about the supervisord server"
  (let [processes (future (get-all-process-info  client))
        state     (future (get-supervisord-state client))]
    {:processes @processes
     :state     @state}))

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

(defn- tail-channel* [[tail-fn read-fn] client name]
  "Creates a channel which will emit bytes from the remote clients process
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (let [log-chan (chan 100)
        control-chan (chan 1)
        wait 1000
        chunk-size 4096
        [content offset _] (tail-fn client name 0 chunk-size)]
    (go-loop [content content
              offset offset]
      (if (empty? content)
        ;; no lines for reading, start over
        (let [[msg ch] (alts! [control-chan (timeout wait)])]
          (if (= control-chan ch)
            (for [ch [log-chan control-chan]] (close! ch))
            (recur (read-fn client name offset chunk-size) offset)))
        ;; process lines, adjust offset, start over
        (let [new-offset (+ offset (count content))]
          (>! log-chan content)
          (recur (read-fn client name new-offset chunk-size) new-offset))))
    [log-chan control-chan]))

(defn process-stdout-chan [client name]
  "Creates a channel which will send bytes from the remote process stdout
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (tail-channel* [tail-process-stdout read-process-stdout] client name))

(defn process-stderr-chan [client name]
  "Creates a channel which will send bytes from the remote process stderr
   Returns [log-chan control-chan].
   Close control-chan to stop the stream"
  (tail-channel* [tail-process-stderr read-process-stderr] client name))

(def api
  {:info        get-supervisord-info
   :status      get-process-info
   :start       start
   :restart     restart
   :stop        stop
   :stop-all    stop-all
   :start-all   start-all
   :status-all  get-all-process-info
   :read-log    read-full-supervisord-log
   :stdout-chan process-stdout-chan
   :stderr-chan process-stderr-chan
   :read-stdout read-full-process-stdout
   :read-stderr read-full-process-stderr
   :tail-stdout tail-process-stdout
   :tail-stderr tail-process-stderr})

(defn supervisor-clients [specs]
  (for [supervisor specs]
    (assoc supervisor
      :client (client supervisor))))

(defn get-supervisor [{:keys [client host name port]}]
  (when client
    (merge {:host host :name name :port port} ;; user provided information
           (get-supervisord-info client))))   ;; + supervisor info

(defn sync-supervisors! [supervisors interval]
  "Start a background process for refreshing
   supervisor information on an interval
   Returns an atom of supervisors"
  (let [supers (atom {:supervisors supervisors})]
    (doall
     (map-indexed
      (fn [idx supervisor]
        (go-loop [data (get-supervisor supervisor)]
          (swap! supers assoc-in [:supervisors idx] data)
          (<! (timeout interval))
          (recur (get-supervisor supervisor))))
      supervisors))
    supers))
