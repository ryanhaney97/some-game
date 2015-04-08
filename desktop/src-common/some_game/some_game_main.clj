(ns some-game.some-game-main
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(declare future-wrap! future-overworld-screen future-battle-screen future-pause-screen screens)

(defgame some-game
  :on-create
  (fn [this]
    (future-wrap!)
    (set-screen! this future-overworld-screen)))

(defn init-namespace [n]
  (require n)
  (refer n))

(init-namespace 'some-game.overworld-screen)
(init-namespace 'some-game.battle-screen)
(init-namespace 'some-game.pause-screen)
(init-namespace 'some-game.screen-handler)

(def future-overworld-screen overworld-screen)
(def future-battle-screen battle-screen)
(def future-pause-screen pause-screen)
(def future-wrap! wrap-screens!)

(def screens {:overworld future-overworld-screen :battle future-battle-screen :pause future-pause-screen})
