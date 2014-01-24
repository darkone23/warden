(ns warden.helpers)

(defn add-class [el class]
  "adds a class-name to a sablano keyword el"
  (keyword (str (name el) "." class)))

(defn external-link [url & content]
  [:a {:href url :target "_blank"} url])

;; helpers for working with font awesome
;; http://fortawesome.github.io/Font-Awesome/

(defn icon-to-name [icon-name]
  (str "fa-" (name icon-name)))

(defn font-icon
  ([icon-key] (font-icon icon-key 1 nil))
  ([icon-key scale] (font-icon icon-key scale nil))
  ([icon-key scale content]
     (let [icon-name (icon-to-name icon-key)
           scale (str "fa-" scale "x")
           class (str "fa." icon-name "." scale)]
       [(add-class :i class) content])))


;; helpers for working with purecss
;; http://purecss.io/

(defn grid [html]
  (update-in html [0] add-class "pure-g"))

(defn responsive-grid [html]
  "adds the purecss class-name for a responsive grid"
  (update-in html [0] add-class "pure-g-r"))

(defn grid-row [html]
  (update-in html [0] add-class (str "pure-u-1")))

(defn grid-unit
  "create a grid unit, either an entire row or size x of n"
  ([html] (update-in html [0] add-class (str "pure-u")))

  ([x n html] (update-in html [0] add-class (str "pure-u-" x "-" n))))

(defn menu [html]
  (update-in html [0] add-class  "pure-menu.pure-menu-fixed.pure-menu-horizontal"))

(defn hide-on-phone [html]
  (update-in html [0] add-class "pure-hidden-phone"))
