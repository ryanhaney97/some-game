(ns some-game.battle-screen
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.repl :refer :all]
            [some-game.battle-functions :refer :all]
            [some-game.utilities :refer :all]))

(declare battle-screen)

(defscreen battle-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (update! screen :camera (orthographic))
    entities)

  :update-entities
  (fn [screen [entities _]]
    (let [player (convert-player (get-in screen [:entities :sprites :player]) screen)
          enemy (convert-enemy (:enemy screen) screen)
          book (make-book screen)
          new-entities (assoc entities
                         :stored-overworld (:entities screen)
                         :sprites {:player player
                                   :enemy enemy
                                   :book book})]
      (.set (.position (:camera screen)) (/ (width screen) 2) (/ (height screen) 2) 0)
      new-entities))

  :on-timer
  (fn [screen [entities _]]
    (condp = (:id screen)
      :book-loaded (assoc-in entities [:sprites :table] (make-spell-list screen (get-in entities [:sprites :player]) (get-in entities [:sprites :book])))
      :cast-animation-complete (do
                                 (reset-animation! (get-animation-from-type (:type @current-attack)))
                                 (-> entities
                                     (remove-projectiles)
                                     (assoc-in [:sprites :player :current-animation] :idle)
                                     ((partial damage-enemy screen))))
      :spawn-fireball (assoc-in entities [:sprites :projectile] (make-fireball (get-in entities [:sprites :player])))
      entities))

  :on-render
  (fn [screen [entities _]]
    (clear!)
    (render! screen (make-renderable entities))
    (->> entities
         (animation-handler screen)
         (handle-projectiles screen)
         (update-positions screen)))

  :on-resize
  (fn [screen [entities _]]
    (size! screen 800 600)
    (graphics! :set-display-mode 800 600 false)
    entities)

  :on-touch-down
  (fn [screen [entities _]]
    (if (get-in entities [:sprites :table])
      (player-turn screen entities)))

  :on-key-up
  (fn [screen [entities _]]
    (cond
     (= (:key screen) (key-code :r)) (change-screen! :overworld))))
