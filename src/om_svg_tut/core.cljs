(ns om-svg-tut.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-svg-tut.events :as events]
            [om-svg-tut.board :as board]
            [clojure.browser.repl :as repl]))

;; (repl/connect "http://localhost:9000/repl")

(enable-console-print!)

(def app-state (atom {:row 0
                      :col 0
                      :value 0
                      :board board/sudoku-board}))

(def square-length 77)
(def number-of-squares 9)
(def squares-per-box 3)
(def box-length (* square-length squares-per-box))
(def board-length (* square-length number-of-squares))

(defn h-line
  "draw horiontal line at given y coord"
  [y-coord]
  (dom/line #js {:x1 0 :y1 y-coord
                 :x2 board-length :y2 y-coord}))

(defn v-line
  "draw vertical line at given y coord"
  [x-coord]
  (dom/line #js {:x1 x-coord :y1 0
                 :x2 x-coord :y2 board-length}))

(defn box-lines
  "the lines in between boxes"
  []
  (let [coords (map #(* 3 square-length %) (range 1 3))]
    (apply dom/g #js {:className "box-lines"}
           (concat (map h-line coords)
                   (map v-line coords)))))

(defn square-lines
  "the lines between the squares"
  []
  (let [coords (map #(* % square-length) (range 1 9))]
    (apply dom/g #js{:className "square-lines"}
           (concat (map h-line coords)
                   (map v-line coords)))))

;; current rowm col and box
(defn current-row
  "highlight given row"
  [row-num]
  (let [y (* row-num square-length)]
    (dom/g #js {:className "current-row"}
           (dom/rect #js {:x 0 :y y
                          :width board-length
                          :height square-length}))))

(defn current-col
  "highlight given column"
  [col-num]
  (let [x (* col-num square-length)]
    (dom/g #js {:className "current-col"}
           (dom/rect #js {:x x :y 0
                          :width square-length
                          :height board-length}))))

(defn current-box
  "highlight given column"
  [[row-num col-num]]
  (let [x (* col-num square-length)
        y (* row-num square-length)]
    (dom/g #js {:className "current-box"}
           (dom/rect #js {:x x :y y
                          :width box-length
                          :height box-length}))))

(defn current-square
  "highlight given square"
  [[row-num col-num]]
  (let [x (* col-num square-length)
        y (* row-num square-length)]
    (dom/g #js {:className "current-square"}
           (dom/rect #js {:x x :y y
                          :width square-length
                          :height square-length}))))

(defn board
  "render svg board"
  [app owner]
  (reify
    om/IRender
    (render [_]
      (let [row (:row app)
            col (:col app)
            start-pos (board/pos->start-pos [col row])]
        (dom/svg #js{:width 693 :height 693}
                 (square-lines)
                 (current-row row)
                 (current-col col)
                 ;;(current-square [row col])
                 (current-box start-pos)
                 (box-lines))))))

(defn object
  "render representation of given object o"
  [o]
  (let [class-name (str "object-" o)]
    (dom/div #js {:className class-name}
             (str o))))

(defn objects
  "render a row of objects"
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:clasName "objects"}
               (apply dom/div #js {:className "object-list"}
                      (map
                       (fn [o] (object o))
                       (range 1 10)))))))

(defn update-pos
  [app key op]
  (om/transact!
   app key
   (fn [val] (mod (op val) number-of-squares))))

(defn game
  "start a new sudoku game"
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [key-chan (om/get-shared owner :keys-chan)]
        (go
          (loop []
            (let [key (<! key-chan)]
              (condp = key
                :right (update-pos app :col inc)
                :left  (update-pos app :col dec)
                :down  (update-pos app :row inc)
                :up    (update-pos app :row dec))
              (recur))))))
    om/IRender
    (render [_]
      (dom/div #js {:className "main"}
               (dom/h1 nil "Sudoku")
               (om/build objects (:value app))
               (om/build board app)))))

(om/root
 game
  app-state
  {:target (. js/document (getElementById "app"))
   :shared {:keys-chan (events/keys-chan)}})