(ns touchtyper.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan >! <!]]
            [domina :refer [append! set-text! set-classes! by-id]]
            [domina.events :refer [listen!]]))

(def key-channel (chan))
(def result-channel (chan))
(def result-src-channel (chan))
(def m-result-src-channel (async/mult result-src-channel))
(def mark-results-channel (chan))
(def generate-results-channel (chan))
(def index-channel (chan))
(async/tap m-result-src-channel mark-results-channel)
(async/tap m-result-src-channel generate-results-channel)

(defn set-class-at-index [class index]
  (set-classes! (by-id (str index)) class))

(def mark-as-current 
  (partial set-class-at-index "current"))

(def mark-as-correct
  (partial set-class-at-index "correct"))

(def mark-as-incorrect
  (partial set-class-at-index "incorrect"))

(defn render-results [{:keys [correct incorrect]}]
  (do
    (set-text! (by-id "correct") correct)
    (set-text! (by-id "incorrect") incorrect)))

(defn mark-results [index result]
  (if (= result :correct)
    (mark-as-correct index)
    (mark-as-incorrect index)))

(defn evaluate-input [[expected actual]]
  (if (= expected actual)
    :correct
    :incorrect))

(let [index (atom 0)]
  (defn put-key-into-channel [text evt]
    (let [key (String/fromCharCode (:charCode evt))
          current-index @index
          expected (nth text current-index)]
      (go (>! key-channel [expected key])
          (>! index-channel current-index))
      (swap! index inc))))

(go (while true
      (mark-results (<! index-channel) (<! mark-results-channel))))

(go (while true
      (render-results (<! result-channel))))

(go (while true
      (>! result-src-channel (evaluate-input (<! key-channel)))))

(let [results (atom {:correct 0 :incorrect 0})]
  (go (while true
        (let [result (<! generate-results-channel)]
          (swap! results (fn [results] (update-in results [result] inc)))
          (>! result-channel @results)))))

(defn render-as-spans [text]
  (let [indexed-text (map-indexed (fn [index char] (str "<span id='" index "'>" char "</span>")) text)]
    (append! (by-id "text") (apply str indexed-text))))

(defn ^:export main []
  (let [text "Hello World!"
        handle-keypress (partial put-key-into-channel text)]
    (do
      (listen! :keypress handle-keypress)
      (render-as-spans text)
      (mark-as-current 0))))
