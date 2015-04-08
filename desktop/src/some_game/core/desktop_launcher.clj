(ns some-game.core.desktop-launcher
  (:require [some-game.some-game-main :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. some-game "some-game" 800 600)
  (Keyboard/enableRepeatEvents true))

(-main)
