(ns broken-spotify.core
  (:require [clj-http.client :as client]))

(def spotify-api-url "https://api.spotify.com/v1/")

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
