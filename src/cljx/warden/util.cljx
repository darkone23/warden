(ns warden.util)

(defn supervisor-id [{:keys [host name port]}]
  "id for referencing a particular supervisor server"
  (str host "-" port "-" name))

(defn key= [x y]
  "Checks that every key in x is equal in y"
  (when (every? true? (for [[k v] x] (= v (get y k)))) y))

(defn filter-key= [m ms]
  "filters a collection of maps by those that match a minimum keyset"
  (filter (partial key= m) ms))

(defn some-key= [m ms]
  "select by minimum keyset"
  (some (partial key= m) ms))
