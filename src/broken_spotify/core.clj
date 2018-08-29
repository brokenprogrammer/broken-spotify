(ns broken-spotify.core
  (:require [clj-http.client :as client]
            [clojure.string :as string]))

(def spotify-api-url "https://api.spotify.com/v1/")
(def spotify-api-token-url "https://accounts.spotify.com/api/token")

(def spotify-path-params [:id :category_id :owner_id :playlist_id :user_id])
(def spotify-query-params [:market :limit :offset :ids])

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
 
(defn replace-path-params
  "TODO: Add documentation."
  [params url]
  (if (string/blank? url)
    ""
    (let [found-keys (select-keys params spotify-path-params)
          split-url (string/split (string/replace url "https://" "") #"/")]
      (str "https://"
        (string/join "/" 
          (map 
            (fn [x] (if (contains? found-keys (keyword x))
              (get found-keys (keyword x))
              x)) split-url))))))

(defn call-builder [method]
  (fn [endpoint m t]
    (client/request 
      (merge
        { :method method
          :url (replace-path-params m (str spotify-api-url endpoint)) 
          :oauth-token t}
        { :query-params (select-keys m spotify-query-params)  
        }))))

(def get-request    (call-builder :get))
(def post-request   (call-builder :post))
(def put-request    (call-builder :put))
(def delete-request (call-builder :delete))

; Album API Endpoints
(def get-an-album 
  "Gets Spotify catalog information for a single album. Takes two
  arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :id, optional key is :market.
  :id The Spotify ID for the album.
  :market An ISO 3166-1 alpha-2 country code.
  
  Example: (get-an-album {:id \"0sNOF9WDwhWunNAHPD3Baj\" :market \"SE\"} \"OAUTH-TOKEN\")"
  (partial get-request "albums/id"))

(def get-an-albums-tracks
  "Gets Spotify catalog information about an album's tracks. Optional
  parameters can be used to limit the number of tracks returned. Takes two
  arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :id, optional keys are :market, :offset & :limit.
  :id The Spotify ID for the album.
  :market An ISO 3166-1 alpha-2 country code.
  :offset The index of the first track to return. Default: 0.
  :limit The maximum number of tracks to return. Default: 20. Minimum: 1. Maximum: 50.
  
  Example: (get-an-albums-tracks {:id \"0sNOF9WDwhWunNAHPD3Baj\" :market \"SE\" :limit 25 :offset 20} \"OAUTH-TOKEN\")"
  (partial get-request "albums/id/tracks"))

(def get-albums
  "Get Spotify catalog information for multiple albums identified by their
  Spotify IDs. Takes two arguments, a map with all the path and query parameters 
  and a oauth-token. Required key in the map is :ids, optional keys are :market.
  :ids A comma-separated list of the Spotify IDs for the albums. Maximum: 20 IDs.
  :market An ISO 3166-1 alpha-2 country code.
  
  Example: (get-albums {:ids \"41MnTivkwTO3UUJ8DrqEJJ,6JWc4iAiJ9FjyK0B59ABb4,6UXCm6bOO4gFlDQZV5yL37\" :market \"SE\"} \"OAUTH-TOKEN\")"
  (partial get-request "albums/"))

; Artist API Endpoints
(def get-an-artist
  ""
  (partial get-request "/artists/id"))

(def get-an-artists-albums
  ""
  (partial get-request "/artists/id/albums"))

(def get-an-artists-top-tracks
  ""
  (partial get-request "/artists/id/top-tracks"))

(def get-an-artists-related-artists
  ""
  (partial get-request "/artists/id/related-artists"))

(def get-artists
  ""
  (partial get-request "/artists"))