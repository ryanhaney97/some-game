(ns some-game.battle-functions
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.utilities :refer :all])
  (:import [com.badlogic.gdx.scenes.scene2d Touchable]))

(def current-attack (atom nil))

(defn swap-touchable! [actor-entity]
  (if (= (actor! actor-entity :get-touchable) Touchable/disabled)
    (actor! actor-entity :set-touchable Touchable/childrenOnly)
    (actor! actor-entity :set-touchable Touchable/disabled)))

(defn get-table-contents [actor-table]
  (map #(.getActor %1) (table! actor-table :get-cells)))

(defn swap-visible! [actor-entity]
  (let [visible? (not (actor! actor-entity :is-visible))]
    (dorun (map #(actor! %1 :set-visible visible?) (get-table-contents actor-entity)))
    (actor! actor-entity :set-visible visible?)))

(defn convert-player [player screen]
  (assoc player
    :x (- (* (width screen) 0.8) (:x (get-entity-center player)))
    :y (- (* (height screen) 0.7) (:y (get-entity-center player)))))

(defn convert-enemy [enemy screen]
  (assoc enemy
    :x (- (* (width screen) 0.2) (:x (get-entity-center enemy)))
    :y (- (* (height screen) 0.7) (:y (get-entity-center enemy)))))

(defn make-spell-list [screen player book]
  (let [spell-names (keys (:spells player))
        spell-name-entities (map #(label %1 (color :white)) spell-names)
        spell-table (table [])]
    (dorun (map #(do (table! spell-table :row)
                   (table! spell-table :add (:object %1))) spell-name-entities))
    (assoc spell-table :priority 1 :x (+ (:x book) (* (texture! book :get-region-width) 0.3)) :y (+ (:y book) (* (texture! book :get-region-height) 0.9)))))

(defn make-book [screen]
  (let [book (texture "book/book.png")
        book-sprite
        (assoc book
          :priority 0
          :animations {:opening (make-animation "book" "opening" 5 (play-mode :normal))}
          :current-animation :opening
          :x (- (* (width screen) 0.5) (:x (get-entity-center book)))
          :y 0)]
    (add-timer! screen :book-loaded (animation! (get-in book-sprite [:animations :opening]) :get-animation-duration))
    book-sprite))

(defn update-positions [screen entities]
  (assoc-in entities [:sprites :book :x] (- (* (width screen) 0.5) (:x (get-entity-center (get-in entities [:sprites :book]))))))

(defn reset-battle! [entities]
  (change-screen! :battle)
  (screen! (get-screen :battle) :update-entities :entities (:stored-overworld entities) :enemy (get-in entities [:sprites :enemy])))

(defn change-to-overworld! [entities]
  (change-screen! :overworld)
  (pass-entities! :overworld entities)
  entities)

(defn damage-enemy [screen entities]
  (let [attack @current-attack
        enemy (get-in entities [:sprites :enemy])
        attacked-enemy (assoc enemy :health (- (:health enemy) (:damage attack)))]
    (if (<= (:health attacked-enemy) 0)
      (change-to-overworld! (normalize-overworld (assoc (:stored-overworld entities) :sprites (dissoc (get-in entities [:stored-overworld :sprites]) :enemy)))))
    (do
      (swap-visible! (get-in entities [:sprites :table]))
      (assoc-in entities [:sprites :enemy] attacked-enemy))))

(defn player-turn [screen entities]
  (let [spell-table (get-in entities [:sprites :table])
        clicked-actor (actor! spell-table :hit (- (:x spell-table) (:input-x screen)) (- (- (height screen) (:input-y screen)) (:y spell-table)) true)]
    (if (not (nil? clicked-actor))
      (do
        (swap-visible! spell-table)
        (reset! current-attack (get (get-in entities [:sprites :player :spells]) (str (label! clicked-actor :get-text))))
        (add-timer! screen :cast-animation-complete (animation! (:cast (get-in entities [:sprites :player :animations])) :get-animation-duration))
        (assoc-in entities [:sprites :player :current-animation] :cast))
      entities)))
