(ns some-game.screen-handler
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.utilities :refer :all]))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!))

  :on-key-up
  (fn [screen entities]
    (if (= (:key screen) (key-code :r))
      (change-screen! :overworld))))

(defn wrap-screens! []
  (set-screen-wrapper! (fn [screen screen-fn]
                         (try (screen-fn)
                           (catch Exception e
                             (.printStackTrace e)
                             (set-screen! (get-game) blank-screen))))))
