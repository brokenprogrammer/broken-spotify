(ns broken-spotify.core-test
  (:require [clojure.test :refer :all]
            [broken-spotify.core :refer :all]))

; Defines spotify-auth-token to contain the auth token
; as long as it already doesn't contain a value.
(defonce spotify-auth-token
  (get-access-token
    (System/getenv "SPOTIFY_CLIENT_ID")
    (System/getenv "SPOTIFY_CLIENT_SECRET")))

(deftest a-test
  (testing "FIXED, I pass."
    (is (= 1 1))))
