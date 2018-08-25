(ns broken-spotify.core
  (:require [clj-http.client :as client]))

(def spotify-api-url "https://api.spotify.com/v1/")
(def spotify-api-token-url "https://accounts.spotify.com/api/token")

(defn get-access-token
  "Requests an access token from Spotify by following the Client Credentials Flow.
  Only endpoints that do not access user information can be accessed using the
  recieved access token. To access user information use OAuth 2 Authorization."
  [client_id client_secret]
  (:access_token (:body 
    (client/post spotify-api-token-url {:form-params {:grant_type "client_credentials"}
                                        :basic-auth [client_id client_secret]
                                        :as :json}))))

(defn refresh-access-token
  "Refreshes an access token using the refresh token that was retrieved during
  the OAuth 2 Authorization."
  [client_id client_secret refresh_token]
  (:access_token (:body
    (client/post spotify-api-token-url {:form-params {:grant_type "refresh_token"
                                                      :refresh_token refresh_token}
                                        :basic-auth [client_id client_secret]
                                        :as :json}))))

(defn get-an-album 
  ""
  [token id] ;TODO: Should take optional argument :market
  (client/get (str spotify-api-url "albums/" id)
    {:query-params {}
     :oauth-token token}))

(defn get-an-albums-tracks
  ""
  [token id] ;TODO: Should take optional arguments: :market :limit :offset
  (client/get (str spotify-api-url "albums/" id "/tracks")
    {:query-params {}
     :oauth-token token}))

(defn get-albums
  ""
  [token ids] ;TODO: Optional argument: :market
  (client/get (str spotify-api-url "albums/")
    {:query-params {:ids (clojure.string/join "," ids)}
     :oauth-token token}))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
