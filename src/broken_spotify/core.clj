(ns broken-spotify.core
  (:require [clj-http.client :as client]
            [clojure.string :as string]))

(def spotify-api-url "https://api.spotify.com/v1/")
(def spotify-api-token-url "https://accounts.spotify.com/api/token")

(def spotify-path-params [:id :category_id :owner_id :playlist_id :user_id])
(def spotify-query-params [:market :limit :offset :ids :q :type])

(defn get-access-token
  "Requests an access token from Spotify by following the Client Credentials Flow.
  Only endpoints that do not access user information can be accessed using the
  recieved access token. To access user information use OAuth 2 Authorization."
  [client_id client_secret]
  (-> (client/post spotify-api-token-url {:form-params {:grant_type "client_credentials"}
                                          :basic-auth [client_id client_secret]
                                          :as :json})
    :body
    :access_token))

(defn refresh-access-token
  "Refreshes an access token using the refresh token that was retrieved during
  the OAuth 2 Authorization."
  [client_id client_secret refresh_token]
  (-> (client/post spotify-api-token-url {:form-params {:grant_type "refresh_token"
                                                        :refresh_token refresh_token}
                                          :basic-auth [client_id client_secret]
                                          :as :json})
      :body
      :access_token))

(defn replace-path-params
  "TODO: Add documentation."
  [params url]
  (if (string/blank? url)
    ""
    (let [found-keys (select-keys params spotify-path-params)
          split-url (string/split (string/replace url "https://" "") #"/")
          replace-key-if-exist (fn [x] (get-in found-keys [(keyword x)] x))]
      (->> (map replace-key-if-exist split-url)
           (string/join "/")
           (str "https://")))))

(defn call-builder [method]
  (fn [endpoint m t]
    (client/request 
      { :method method
        :content-type :json ;TODO: In future when images are being sent there needs to be a check here.
        :url (replace-path-params m (str spotify-api-url endpoint)) 
        :oauth-token t
        :query-params (select-keys m spotify-query-params)  
      })))

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
  "Get Spotify catalog information for a single artist identified by their 
  unique Spotify ID. Takes two arguments, a map with all the path and query parameters 
  and a oauth-token. Required key in the mas is :id.
  :id The Spotify ID for the artist.
  
  Example: (get-an-artist {:id \"0OdUWJ0sBjDrqHygGUXeCF\"} \"OAUTH-TOKEN\")"
  (partial get-request "artists/id"))

(def get-an-artists-albums
  "Get Spotify catalog information about an artist’s albums. Takes two 
  arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :id, optional keys are :include_groups	:market :limit
  and :offset.
  :id The Spotify ID for the artist.
  :include_groups A comma-separated list of keywords that will be used to filter the response.
  :market An ISO 3166-1 alpha-2 country code.
  :limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first album to return. Default: 0
  
  Example: (get-an-artists-albums {:id \"0OdUWJ0sBjDrqHygGUXeCF\" :market \"SE\" :include_groups [\"single\"] :limit 2 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "artists/id/albums"))

(def get-an-artists-top-tracks
  "Get Spotify catalog information about an artist’s top tracks by country.
  Takes two arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :id, optional key is :market.
  :id The Spotify ID for the artist.
  :market An ISO 3166-1 alpha-2 country code.
  
  Example: (get-an-artists-top-tracks {:id \"0OdUWJ0sBjDrqHygGUXeCF\" :market \"SE\"} \"OAUTH-TOKEN\")"
  (partial get-request "artists/id/top-tracks"))

(def get-an-artists-related-artists
  "Get Spotify catalog information about artists similar to a given artist. 
  Takes two arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :id.
  :id The Spotify ID for the artist.
  
  Example: (get-an-artists-related-artists {:id \"0OdUWJ0sBjDrqHygGUXeCF\"} \"OAUTH-TOKEN\")"
  (partial get-request "artists/id/related-artists"))

(def get-artists
  "Get Spotify catalog information for several artists based on their Spotify IDs. Takes
  two arguments, a map with all the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs for the artists. Maximum: 50 IDs.
  
  Example: (get-artists {:ids \"0oSGxfWSnnOXhD2fKuz2Gy,3dBVyJ7JuOMt4GE9607Qin\"]} \"OAUTH-TOKEN\")"
  (partial get-request "artists/"))

; Browse API Endpoints
(def get-a-category
  "Get a single category used to tag items in Spotify. Takes two
  arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :category_id, optional paramenters are :country and :locale.
  :category_id The Spotify category ID for the category.
  :country A country: an ISO 3166-1 alpha-2 country code.
  :locale The desired language, consisting of an ISO 639-1 language code and an 
    ISO 3166-1 alpha-2 country code, joined by an underscore. 
  
  Example: (get-a-category {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\"} \"OAUTH-TOKEN\")"
  (partial get-request "browse/categories/category_id"))

(def get-a-categorys-playlists
  "Get a list of Spotify playlists tagged with a particular category. Takes two
  arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :category_id, optional paramenters are :country, :limit and :offset.
  :category_id The Spotify category ID for the category.
  :country A country: an ISO 3166-1 alpha-2 country code.
  :limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first album to return. Default: 0.

  Example: (get-a-categorys-playlists {:category_id \"party\" :country \"BR\" :limit 2 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "browse/categories/category_id/playlists"))

(def get-categories
  "Get a list of categories used to tag items in Spotify. Takes two
  arguments, a map with the path and query parameters and a oauth-token.
  Optional parameters are :country, :locale, :limit and :offset.
  :country A country: an ISO 3166-1 alpha-2 country code.
  :locale The desired language, consisting of an ISO 639-1 language code and an 
    ISO 3166-1 alpha-2 country code, joined by an underscore. 
  :limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first album to return. Default: 0.
  
  Example: (get-categories {:country \"SE\" :locale \"sv_SE\" :limit 10 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "browse/categories"))

(def get-featured-playlists
  "Get a list of Spotify featured playlists. Takes two
  arguments, a map with the path and query parameters and a oauth-token.
  Optional parameters are :locale, :country, :timestamp, :limit and :offset.
  :locale The desired language, consisting of an ISO 639-1 language code and an 
    ISO 3166-1 alpha-2 country code, joined by an underscore.
  :country A country: an ISO 3166-1 alpha-2 country code.
  :timestamp A timestamp in ISO 8601 format: yyyy-MM-ddTHH:mm:ss.
  :limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first album to return. Default: 0.
  
  Example: (get-featured-playlists {:country \"SE\" :locale \"sv_SE\" :timestamp \"2014-10-23T09:00:00\" :limit 2 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "browse/featured-playlists"))

(def get-new-releases
  "Get a list of new album releases featured in Spotify. Takes two
  arguments, a map with the path and query parameters and a oauth-token.
  Optional parameters are :country, :limit and :offset.
  :country A country: an ISO 3166-1 alpha-2 country code.
  :limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first album to return. Default: 0.
  
  Example: (get-new-releases {:country \"SE\" :limit 10 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "browse/new-releases"))

; TODO: Unimplemented
(def get-recommendations
  ""
  (partial get-request "recommendations/"))

; Follow API Endpoints
(def user-follows-artist-or-users?
  "Check to see if the current user is following one or more artists or other Spotify users.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required keys in the map is :ids and :type.
  :ids A comma-separated list of the artist or the user Spotify IDs to check.
  :type The ID type, either artist or user.
  
  Example: (user-follows-artist-or-user? {:ids \"74ASZWbe4lXaubB36ztrGX,08td7MxkoHQkXnWAYD8d6\" :type \"artist\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/following/contains"))

(def user-follows-playlist?
  "Check to see if one or more Spotify users are following a specified playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required keys in the map is :ids, :owner_id and :playlist_id.
  :ids A comma-separated list of the artist or the user Spotify IDs to check.
  :owner_id The Spotify user ID of the person who owns the playlist.
  :playlist_id The Spotify ID of the playlist.

  Example: (user-follows-playlist? {:ids \"possan,elogain\" :owner_id \"OWNER_ID\" :playlist_id \"2v3iNvBX8Ay1Gt2uXtUKUT\"} \"OAUTH-TOKEN\")"
  (partial get-request "users/owner_id/playlists/playlist_id/followers/contains"))

(def follow-artist-or-user
  "Add the current user as a follower of one or more artists or other Spotify users.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required keys in the map is :ids and :type.
  :ids A comma-separated list of the artist or the user Spotify IDs to check.
  :type The ID type, either artist or user.

  Example: (follow-artist-or-user {:ids \"exampleuser01\" :type \"user\"} \"OAUTH-TOKEN\")"
  (partial put-request "me/following"))

(def follow-playlist
  "Add the current user as a follower of a playlist. Takes two arguments,
  a map with the path and query parameters and a oauth-token. Required key in
  map is :playlist_id.
  :playlist_id The Spotify ID of the playlist, Any playlist can be followed,
    regardless of its public/private status, as long as you know its playlist ID.

  Example: (follow-playlist {:playlist_id \"2v3iNvBX8Ay1Gt2uXtUKUT\"} \"OAUTH-TOKEN\")"
  (partial put-request "playlists/playlist_id/followers"))

(def get-followed-artists
  "Get the current user’s followed artists. Takes two arguments, a map with the
  path and query parameters and a oauth-token. Required key in the map is :type and
  optional keys are :limit and :after.
  :type The ID type: currently only artist is supported.
  :limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
  :after The last artist ID retrieved from the previous request.

  Example: (get-followed-artists {:type \"artist\" :limit 50 :after 50} \"OAUTH-TOKEN\")"
  (partial get-request "me/following"))
  
(def unfollow-artist-or-users
  "Remove the current user as a follower of one or more artists or other Spotify users.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required keys in the map are :ids and :type.
  :ids A comma-separated list of the artist or the user Spotify IDs to check.
  :type The ID type: either artist or user.

  Example: (unfollow-artist-or-users {:ids \"exampleuser01\" :type \"user\"} \"OAUTH-TOKEN\")"
  (partial delete-request "me/following"))

(def unfollow-playlist
  "Remove the current user as a follower of a playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id.
  :playlist_id The Spotify ID of the playlist that is to be no longer followed.

  Example: (unfollow-playlist {:playlist_id \"2v3iNvBX8Ay1Gt2uXtUKUT\"} \"OAUTH-TOKEN\")"
  (partial delete-request "playlists/playlist_id/followers"))

; Library API Endpoints
(def is-saved-albums? 
  ""
  (partial get-request "me/albums/contains"))

(def is-saved-tracks?
  ""
  (partial get-request "me/tracks/contains"))

(def get-saved-albums
  ""
  (partial get-request "me/albums"))

(def get-saved-tracks
  ""
  (partial get-request "me/tracks"))

;TODO: Implement, function that builds parameters will get confused of ids=ids
(def remove-saved-albums
  ""
  (partial delete-request "me/albums?ids=ids"))

(def remove-saved-tracks
  ""
  (partial delete-request "me/tracks"))

;TODO: Implement
(def save-albums
  ""
  (partial put-request "me/albums?ids=ids"))

(def save-tracks
  ""
  (partial put-request "me/tracks"))

; Personalization API Endpoints
;TODO: Implement, api params doesn't support type yet.
(def get-users-top-artist-and-tracks
  ""
  (partial get-request "me/top/type"))

; Player API Endpoints
(def get-available-devices
  ""
  (partial get-request "me/player/devices"))

(def get-current-playback-info
  ""
  (partial get-request "me/player"))

(def get-recently-played-tracks
  ""
  (partial get-request "me/player/recently-played"))

(def get-currently-playing-track
  ""
  (partial get-request "me/player/currently-playing"))

(def pause-user-playback
  ""
  (partial put-request "me/player/pause"))

(def player-seek-to
  ""
  (partial put-request "me/player/seek"))

(def player-toggle-repeat
  ""
  (partial put-request "me/player/repeat"))

(def player-set-volume
  ""
  (partial put-request "me/player/volume"))

(def player-next-track
  ""
  (partial post-request "me/player/next"))

(def player-previous-track
  ""
  (partial post-request "me/player/previous"))

(def player-start-resume-playback
  ""
  (partial put-request "me/player/play"))

(def player-toggle-shuffle
  ""
  (partial put-request "me/player/shuffle"))

(def player-transfer-playback
  ""
  (partial put-request "me/player"))

; Playlists API Endpoints
(def add-tracks-to-playlist
  ""
  (partial post-request "playlists/playlist_id/tracks"))

(def change-playlist-details
  ""
  (partial put-request "playlists/playlist_id"))

(def create-playlist
  ""
  (partial post-request "users/user_id/playlists"))

(def get-playlists
  ""
  (partial get-request "me/playlists"))

(def get-users-playlists
  ""
  (partial get-request "users/user_id/playlists"))

(def get-playlist-cover-image
  ""
  (partial get-request "playlists/playlist_id/images"))

(def get-playlist
  ""
  (partial get-request "playlists/playlist_id"))

(def get-playlist-tracks
  ""
  (partial get-request "playlists/playlist_id/tracks"))

(def remove-tracks-from-playlist
  ""
  (partial delete-request "playlists/playlist_id/tracks"))

(def reorder-playlist-tracks
  ""
  (partial put-request "playlists/playlist_id/tracks"))

(def replace-playlist-tracks
  ""
  (partial put-request "playlists/playlist_id/tracks"))

(def upload-playlist-cover-image
  ""
  (partial put-request "playlists/playlist_id/images"))

; Search API Endpoints
(def search
  "Get Spotify Catalog information about artists, albums, tracks or playlists that match a keyword string.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required keys in map are :q and :type, optional keys are :market, :limit and :offset.
  :q Search query keywords and optional field filters and operators. 
  :type A comma-separated list of item types to search across. Valid types are: album , artist, playlist, and track.
  :market An ISO 3166-1 alpha-2 country code.
  :limit Maximum number of results to return. Default: 20 Minimum: 1 Maximum: 50. 
  :offset The index of the first result to return. Default: 0. Maximum offset: 10,000. 

  Example: (search {:q \"tania bowra\" :type \"artist\"  :market \"SE\"  :limit 50 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "search/"))

; Tracks API Endpoints
(def get-audio-analysis
  ""
  (partial get-request "audio-analysis/id"))

(def get-track-audio-features
  ""
  (partial get-request "audio-features/id"))

(def get-tracks-audio-features
  ""
  (partial get-request "audio-features/"))

(def get-tracks
  ""
  (partial get-request "tracks/"))

(def get-track
  ""
  (partial get-request "tracks/id"))

; Users Profile API Endpoints
(def get-current-user-profile
  ""
  (partial get-request "me/"))

(def get-user-profile
  ""
  (partial get-request "users/user_id"))
