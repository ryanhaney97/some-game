(ns some-game.battle-functions
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.utilities :refer :all])
  (:import [com.badlogic.gdx.scenes.scene2d Touchable]
           [java.lang Math]))

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

(defn remove-projectiles [entities]
  (if (get-in entities [:sprites :projectile])
    (assoc entities :sprites (dissoc (:sprites entities) :projectile))
    entities))

(defn convert-player [player screen]
  (assoc player
    :x (- (* (width screen) 0.8) (:x (get-entity-center player)))
    :y (- (* (height screen) 0.7) (:y (get-entity-center player)))))

(defn convert-enemy [enemy screen]
  (assoc enemy
    :x (- (* (width screen) 0.2) (:x (get-entity-center enemy)))
    :y (- (* (height screen) 0.7) (:y (get-entity-center enemy)))))

(defn change-to-overworld! [entities]
  (change-screen! :overworld)
  (pass-entities! :overworld entities)
  entities)

(defn make-spell-list [screen player book]
  (let [spells (:spells player)
        spell-name-entities (map #(label (key %1) (color :white)) (reverse (sort-by :damage spells)))
        spell-table (table [])]
    (dorun (map #(do (table! spell-table :row)
                   (table! spell-table :add (:object %1))) spell-name-entities))
    (assoc spell-table :priority 1 :x (+ (:x book) (* (texture! book :get-region-width) 0.3)) :y (+ (:y book) (* (texture! book :get-region-height) 0.9)))))

(defn make-book [screen]
  (reset-animation! :opening)
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

(defn make-fireball [owner]
  (reset-animation! :grow)
  (reset-animation! :explode)
  (assoc (texture "fireball/grow/grow1.png" :flip (texture! owner :is-flip-x) false)
    :x (:x owner)
    :y (+ (:y owner) (:y (get-entity-center owner)))
    :vx (if (texture! owner :is-flip-x) -5 5)
    :vy 0
    :animations {:grow (make-animation "fireball" "grow" 3 (play-mode :loop) 0.13)
                 :explode (make-animation "fireball" "explode" 2 (play-mode :normal))}
    :current-animation :grow
    :owner owner
    :priority 3))

(defn colliding? [entity1 entity2]
  (let [center1 (get-entity-center entity1)
        center2 (get-entity-center entity2)
        position1 (+ (:x entity1) (:x center1))
        position2 (+ (:x entity2) (:x center2))
        distance (Math/abs (- position1 position2))]
    (>= 5 distance)))

(defn damage-enemy [screen entities]
  (let [attack @current-attack
        enemy (get-in entities [:sprites :enemy])
        attacked-enemy (assoc enemy :health (- (:health enemy) (:damage attack)))]
    (if (<= (:health attacked-enemy) 0)
      (change-to-overworld! (normalize-overworld (assoc (:stored-overworld entities) :sprites (dissoc (get-in entities [:stored-overworld :sprites]) :enemy)))))
    (do
      (swap-visible! (get-in entities [:sprites :table]))
      (assoc-in entities [:sprites :enemy] attacked-enemy))))

(defn handle-projectiles [screen entities]
  (let [projectile (get-in entities [:sprites :projectile])]
    (if projectile
      (let [owner (if (texture! projectile :is-flip-x) :player :enemy)
            enemy (if (texture! projectile :is-flip-x) :enemy :player)]
        (if (and (not= 0 (:vx projectile)) (colliding? projectile (get-in entities [:sprites enemy])))
          (do
            (add-timer! screen :cast-animation-complete (animation! (:explode (:animations projectile)) :get-animation-duration))
            (-> entities
                (assoc-in [:sprites :projectile :vx] 0)
                (assoc-in [:sprites :projectile :y] (get-in entities [:sprites enemy :y]))
                (assoc-in [:sprites :projectile :current-animation] :explode)))
          (assoc-in entities [:sprites :projectile :x] (+ (:vx projectile) (:x projectile)))))
      entities)))

(defn update-positions [screen entities]
  (assoc-in entities [:sprites :book :x] (- (* (width screen) 0.5) (:x (get-entity-center (get-in entities [:sprites :book]))))))

(defn reset-battle! [entities]
  (change-screen! :battle)
  (screen! (get-screen :battle) :update-entities :entities (:stored-overworld entities) :enemy (get-in entities [:sprites :enemy])))

(defn get-animation-from-type [attack-type]
  (condp = attack-type
    :fire :cast-fire
    :cast))

(defn get-event-from-type [attack-type]
  (condp = attack-type
    :fire :spawn-fireball
    :cast-animation-complete))

(defn player-turn [screen entities]
  (let [spell-table (get-in entities [:sprites :table])
        clicked-actor (actor! spell-table :hit (- (:x spell-table) (:input-x screen)) (- (- (height screen) (:input-y screen)) (:y spell-table)) true)]
    (if (not (nil? clicked-actor))
      (let [attack (get (get-in entities [:sprites :player :spells]) (str (label! clicked-actor :get-text)))
            attack-animation (get-animation-from-type (:type attack))]
        (swap-visible! spell-table)
        (reset! current-attack attack)
        (add-timer! screen (get-event-from-type (:type attack)) (animation! (attack-animation (get-in entities [:sprites :player :animations])) :get-animation-duration))
        (assoc-in entities [:sprites :player :current-animation] attack-animation))
      entities)))
