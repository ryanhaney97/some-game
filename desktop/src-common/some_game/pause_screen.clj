(ns some-game.pause-screen
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.repl :refer :all]
            [some-game.pause-functions :refer :all]
            [some-game.utilities :refer :all]))

(declare pause-screen)

(defscreen pause-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (make-menu screen))

  :on-render
  (fn [screen entities]
    (render! screen entities))

  :on-resize
  (fn [screen entities]
    (size! screen 800 600)))
