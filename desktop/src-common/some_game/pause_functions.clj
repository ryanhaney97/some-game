(ns some-game.pause-functions
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.utilities :refer :all]))

(def init-menu
  (memoize
   (fn [width height]
     [(assoc (texture "pause-background.png")
        :x (- (/ width 2) 125)
        :y (- (/ height 2) 200))])))

(def screen->menu
  (memoize
   (fn [screen-width screen-height x y]
     {:x (+ (- (/ (width screen-width) 2) 125) x)
      :y (+ (- (/ (height screen-height) 2) 200) y)})))

(def screen->menu-x
  (memoize
   (fn [screen-width x]
     (:x (screen->menu screen-width 0 x 0)))))

(def screen->menu-y
  (memoize
   (fn [screen-height y]
     (:y (screen->menu 0 screen-height 0 y)))))

(def test-label
  (memoize
   (fn [width height]
     (assoc (label "test" (color :black))
       :x (screen->menu-x width 0)
       :y (screen->menu-y height 0)))))

(defn make-menu [screen]
  [(init-menu (width screen) (height screen)) (test-label (width screen) (height screen))])
