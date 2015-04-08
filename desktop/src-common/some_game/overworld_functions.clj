(ns some-game.overworld-functions
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.utilities :refer :all])
  (:import [java.lang Math]))

(defn make-player [screen]
  (let [player (texture "player/idle/idle1.png")]
    (-> player
        (align-center screen)
        (assoc
          :priority 2
          :animations {:walking (make-animation "player" "walking" 2 (play-mode :loop))
                       :idle (make-animation "player" "idle" 1 (play-mode :normal))
                       :cast (make-animation "player" "cast" 3 (play-mode :normal) 0.3)}
          :current-animation :idle
          :health 20
          :stats {:max-health 20
                  :memory 1
                  :willpower 1
                  :efficiency 1
                  :sharpness 1
                  :id 1
                  :ego 1
                  :superego 1}
          :spells (merge {} (make-spell "fireball" 3 :other) (make-spell "something" 5 :other))
          :vx 0
          :vy 0))))

(defn make-world [screen]
  (let [world (texture "world.png")]
    (-> world
        (align-center screen)
        (assoc
          :priority 0))))

(defn make-enemy [screen player]
  (let [enemy (texture "enemy.png")]
    (-> enemy
        (assoc
          :priority 1
          :x (+ (:x (get-entity-center player)) (- (rand-int 500) 250))
          :y (+ (:y (get-entity-center player)) (- (rand-int 500) 250))
          :health 20
          :stats {:max-health 20
                  :memory 1
                  :willpower 1
                  :efficiency 1
                  :sharpness 1
                  :id 1
                  :ego 1
                  :superego 1}
          :spells (merge {} (make-spell "fireball" 3 :fire))
          :vx 0
          :vy 0))))

(defn apply-velocity [entity]
  (assoc entity :x (+ (:vx entity) (:x entity)) :y (+ (:vy entity) (:y entity))))

(defn apply-velocities [entities]
  (let [entities-with-velocity (into {} (filter #(:vx (second %1)) (:sprites entities)))
        new-entities (map #(if (contains? entities-with-velocity (first %1)) [(first %1) (apply-velocity (second %1))] %1) (:sprites entities))]
    (assoc entities :sprites (apply merge (map #(zipmap [(first %1)] [(second %1)]) (map flatten new-entities))))))

(defn change-player [entities function]
  (into (filter (complement :player) entities) (map function (filter :player entities))))

(defn is-pause? [entity]
  (and (:data entity) (= (:data entity) :pause)))

(defn change-pause [entities function]
  (into (filter (complement is-pause?) entities) (map function (filter is-pause? entities))))

(defn colliding? [entity1 entity2 world]
  (let [center1 (get-entity-center entity1)
        center2 (get-entity-center entity2)
        position1 (get-world-coords entity1 world)
        position2 (get-world-coords entity2 world)
        distance (get-distance position1 position2)
        colliding-distance (Math/abs (+ (:x center1) (:x center2)))]
    (>= colliding-distance distance)))

(defn get-collisions [entities]
  (let [world (:world entities)]
    (first (into #{}
                 (for [entity1 (dissoc entities :world)
                       entity2 (dissoc entities :world)
                       :when (not= entity1 entity2)]
                   (if (colliding? (val entity1) (val entity2) world) (merge {} entity1 entity2)))))))

(defn change-to-battle! [entities enemy]
  (change-screen! :battle)
  (if (not (texture! (get-in entities [:sprites :player]) :is-flip-x))
    (texture! (get-in entities [:sprites :player]) :flip true false))
  (screen! (get-screen :battle) :update-entities :entities (assoc-in entities [:sprites :player :current-animation] :idle) :enemy enemy))

(defn handle-collisions [ent]
  (let [entities (:sprites ent)
        player (:player entities)
        collisions (get-collisions entities)
        battle-enemy (:enemy collisions)]
    (if (not (nil? battle-enemy))
      (change-to-battle! ent battle-enemy)
      ent)))

(defn screen-pause! [entities]
  (change-screen! :overworld :pause)
  (pass-entities! :overworld entities)
  entities)

(defn reset-overworld! [entities]
  (change-screen! :overworld)
  (pass-entities! :overworld entities)
  entities)

(defn handle-pause [entities]
  (if (:paused? (:data entities))
    (screen-pause! entities)
    (reset-overworld! entities)))

(defn pause [entities]
  (-> (assoc entities :data (assoc (:data entities) :paused? (not (:paused? (:data entities)))))
      (handle-pause)))

(defn on-key [pressed-key direction sign]
  (fn [entities is-pressed?]
    (let [sprites (:sprites entities)
          data (:data entities)
          amount-changed (if is-pressed? (sign 3) (sign -3))]
      (if (and (= direction :vx) (not= (+ (get-in entities [:sprites :player direction]) amount-changed) 0))
       (if (and (< (+ (get-in entities [:sprites :player direction]) amount-changed) 0) (not (texture! (get-in entities [:sprites :player]) :is-flip-x)))
        (texture! (get-in entities [:sprites :player]) :flip true false)
        (if (and (> (+ (get-in entities [:sprites :player direction]) amount-changed) 0) (texture! (get-in entities [:sprites :player]) :is-flip-x))
          (texture! (get-in entities [:sprites :player]) :flip true false))))
      (-> entities
          (assoc-in [:sprites :player direction] (+ (get-in entities [:sprites :player direction]) amount-changed))
          (assoc-in [:data :camera direction] (+ (get-in entities [:data :camera direction]) amount-changed))
          (assoc-in [:data :keys pressed-key] is-pressed?)
          (#(assoc-in %1 [:sprites :player :current-animation] (if (and (= (get-in %1 [:sprites :player :vx]) 0) (= (get-in %1 [:sprites :player :vy]) 0)) :idle :walking)))))))

(def on-key-w (on-key :w :vy +))
(def on-key-s (on-key :s :vy -))
(def on-key-d (on-key :d :vx +))
(def on-key-a (on-key :a :vx -))
