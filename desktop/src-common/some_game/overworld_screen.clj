(ns some-game.overworld-screen
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.repl :refer :all]
            [some-game.overworld-functions :refer :all]
            [some-game.utilities :refer :all]))

(declare overworld-screen)

(defscreen overworld-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (update! screen :camera (orthographic))
    (let [world (make-world screen)
          player (make-player screen)
          enemy (make-enemy screen player)]
      {:data {:camera {:vx 0 :vy 0} :paused? false :keys {:w false :a false :s false :d false}} :sprites {:world world :enemy enemy :player player}}))

  :update-entities
  (fn [screen [entities _]]
    (center-on! (:camera screen) (get-in (:entities screen) [:sprites :player]))
    (:entities screen))

  :get-entities
  (fn [screen [entities _]]
    entities)

  :on-render
  (fn [screen [entities _]]
    (clear!)
    (render! screen (make-renderable entities))
    (if (not (get-in entities [:data :paused?] false))
      (do
        (orthographic! screen :translate (get-in entities [:data :camera :vx] 0) (get-in entities [:data :camera :vy] 0))
        (->> entities
             (apply-velocities)
             (animation-handler screen)
             (handle-collisions)))
      entities))

  :on-resize
  (fn [screen entities]
    (size! screen 800 600)
    (graphics! :set-display-mode 800 600 false)
    entities)

  :on-key-down
  (fn [screen [entities _]]
    (let [sprites (:sprites entities)
          data (:data entities)]
      (cond
       (and (= (:key screen) (key-code :s)) (not (:s (:keys data)))) (on-key-s entities true)

       (and (= (:key screen) (key-code :w)) (not (:w (:keys data)))) (on-key-w entities true)

       (and (= (:key screen) (key-code :a)) (not (:a (:keys data)))) (on-key-a entities true)

       (and (= (:key screen) (key-code :d)) (not (:d (:keys data)))) (on-key-d entities true))))

  :on-key-up
  (fn [screen [entities _]]
    (let [sprites (:sprites entities)
          data (:data entities)]
      (cond
       (and (= (:key screen) (key-code :s)) (:s (:keys data))) (on-key-s entities false)

       (and (= (:key screen) (key-code :w)) (:w (:keys data))) (on-key-w entities false)

       (and (= (:key screen) (key-code :a)) (:a (:keys data))) (on-key-a entities false)

       (and (= (:key screen) (key-code :d)) (:d (:keys data))) (on-key-d entities false)

       (= (:key screen) (key-code :r)) (change-screen! :overworld)

       (= (:key screen) (key-code :e)) (pause entities)))))
