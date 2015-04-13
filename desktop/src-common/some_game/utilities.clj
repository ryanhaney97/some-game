(ns some-game.utilities
  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [some-game.some-game-main :refer [some-game
                                              screens]]))

(def animation-start-times (atom {}))

(defn change-screen! [& requested-screens]
  (reset! animation-start-times {})
  (apply set-screen! some-game (map #(get screens %1) requested-screens)))

(defn get-game []
  some-game)

(defn get-screen [screen]
  (get screens screen))

(defn pass-entities! [screen entities]
  (screen! (get-screen screen) :update-entities :entities entities))

(defn align-center [entity screen]
  (assoc entity :x (- (/ (width screen) 2.0) (/ (texture! entity :get-region-width) 2.0)) :y (- (/ (height screen) 2.0) (/ (texture! entity :get-region-height) 2.0))))

(defn get-entity-center [entity]
  {:x (/ (texture! entity :get-region-width) 2.0) :y (/ (texture! entity :get-region-height) 2.0)})

(defn make-animation
  ([entity-name general-name frames pm frame-duration]
   (animation frame-duration (into [] (map #(texture (str entity-name "/" general-name "/" general-name "" %1 ".png")) (range 1 (inc frames)))) :set-play-mode pm))
  ([entity-name general-name frames pm]
   (make-animation entity-name general-name frames pm 0.2)))

(defn reset-animation! [animation-name]
  (if (get @animation-start-times animation-name)
   (swap! animation-start-times dissoc animation-name)))

(defn center-on! [camera entity]
  (.set (.position camera) (+ (:x entity) (:x (get-entity-center entity))) (+ (:y entity) (:y (get-entity-center entity))) 0))

(defn animate [screen entity]
  (let [animation-name (:current-animation entity)
        current-animation (get (:animations entity) (:current-animation entity))]
    (if (not (get @animation-start-times animation-name))
      (swap! animation-start-times assoc animation-name (:total-time screen)))
    (assoc entity :object (animation! current-animation :get-key-frame (- (:total-time screen) (animation-name @animation-start-times)) (not= (animation! current-animation :get-play-mode) (play-mode :normal))))))

(defn apply-animation [screen entity]
  (if (texture! entity :is-flip-x)
    (let [animated (animate screen entity)]
      (if (not (texture! animated :is-flip-x))
        (do (texture! animated :flip true false)
          animated)
        (animate screen entity)))
    (let [animated (animate screen entity)]
      (if (texture! animated :is-flip-x)
        (do (texture! animated :flip true false)
          animated)
        (animate screen entity)))))

(defn on-animation-complete [screen entity-name entity]
  entity)

(defn animation-handler [screen entities]
  (let [entities-with-animation (into {} (filter #(:current-animation (second %1)) (:sprites entities)))
        new-entities (map #(if (contains? entities-with-animation (first %1)) [(first %1) (apply-animation screen (second %1))] %1) (:sprites entities))
        player (get-in entities [:sprites :player])]
    (assoc entities :sprites (apply merge (map #(zipmap [(first %1)] [(second %1)]) (map flatten new-entities))))))

(defn get-world-coords [entity world]
  (let [center (get-entity-center entity)]
    {:x (Math/abs (- (:x world) (+ (:x entity) (:x center)))) :y (Math/abs (- (:y world) (+ (:y entity) (:y center))))}))

(def get-distance
  (memoize
   (fn [coord1 coord2]
     (let [x1 (min (:x coord1) (:x coord2))
           x2 (max (:x coord1) (:x coord2))
           delta-x (- x2 x1)
           y1 (min (:y coord1) (:y coord2))
           y2 (max (:y coord1) (:y coord2))
           delta-y (- y2 y1)]
       (Math/sqrt (+ (* delta-x delta-x) (* delta-y delta-y)))))))

(defn velocity? [entity]
  (and (:vx entity) (:vy entity)))

(defn make-renderable [entities]
  (->> entities
       (:sprites)
       (vals)
       (sort-by :priority)))

(defn make-spell [spell-name spell-base-damage spell-type]
  {spell-name {:damage spell-base-damage :type spell-type}})

(defn normalize-overworld [entities]
  (-> entities
      (assoc-in [:sprites :player :vx] 0)
      (assoc-in [:sprites :player :vy] 0)
      (assoc-in [:data :camera :vx] 0)
      (assoc-in [:data :camera :vy] 0)
      (assoc-in [:data :keys] {:w false :a false :s false :d false})))
