(ns warden.font-awesome)

(defn add-class [el class]
  "adds a class-name to a sablono html vector"
  (-> (name el) (str "." class) keyword))

(defn key-to-name [icon-name]
  (str "fa-" (name icon-name)))

(defn font-icon 
  ([icon-key] (font-icon icon-key 1 nil))
  ([icon-key scale] (font-icon icon-key scale nil))
  ([icon-key scale content]
     (let [icon-name (key-to-name icon-key)
           scale (str "fa-" scale "x")
           class (str "fa." icon-name "." scale)]
       [(add-class :i class) content])))

