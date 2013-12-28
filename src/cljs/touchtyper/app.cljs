(ns touchtyper.app
  (:require [domina :refer [append! set-text! set-classes! by-id]]
            [domina.events :refer [listen!]]))

(def index (atom 0))

(defn render-as-spans [text]
  (let [indexed-text (map-indexed (fn [index char] (str "<span id='" index "'>" char "</span>")) text)]
    (append! (by-id "text") (apply str indexed-text))))

(defn set-class-at-index [class index]
  (set-classes! (by-id (str index)) class))

(def mark-as-current 
  (partial set-class-at-index "current"))

(def mark-as-correct
  (partial set-class-at-index "correct"))

(def mark-as-incorrect
  (partial set-class-at-index "incorrect"))

(defn show-key [{charcode :charCode}]
  (set-text! (by-id "content") (String/fromCharCode charcode)))

(defn handle-keypress [text {charcode :charCode}]
  (let [current-index @index
        current-char (nth text current-index)]
    (if (= current-char (String/fromCharCode charcode))
      (mark-as-correct current-index)
      (mark-as-incorrect current-index))
    (mark-as-current (swap! index inc))))

(defn ^:export main []
  (let [text "Hello World!"
        handle-keypress (partial handle-keypress text)]
    (do
      (listen! :keypress handle-keypress)
      (render-as-spans text)
      (mark-as-current 0))))
