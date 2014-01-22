(ns warden.pure)

;; facilities for working with purecss
;; http://purecss.io/

(defn add-class [el class]
  "adds a class-name to a sablono html vector"
  (-> (name el) (str "." class) keyword))

(defn responsive-grid [html]
  "adds the purecss class-name for a responsive grid"
  (update-in html [0] add-class "pure-g-r"))

(defn grid-unit
  "create a grid unit, either an entire row or size x of n"
  ([html] (update-in html [0] add-class (str "pure-u-1")))
  ([x n html] (update-in html [0] add-class (str "pure-u-" x "-" n))))
