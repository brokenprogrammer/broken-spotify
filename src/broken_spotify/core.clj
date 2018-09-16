(ns broken-spotify.core
  (:require [clj-http.client :as client]
            [clojure.string :as string]))

(def spotify-api-url "https://api.spotify.com/v1/")
(def spotify-api-token-url "https://accounts.spotify.com/api/token")

(def spotify-path-params [:id :category_id :owner_id :playlist_id :user_id])
(def spotify-query-params [:market :limit :offset :ids :q :type :device_id :position_ms :state :volume_percent :position :uris :fields])

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
  "Check if one or more albums is already saved in the current Spotify user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs for the albums. Maximum: 50 IDs.

  Example: (is-saved-albums? {:ids \"0pJJgBzj26qnE1nSQUxaB0,5ZAKzV4ZIa5Gt7z29OYHv0\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/albums/contains"))

(def is-saved-tracks?
  "Check if one or more tracks is already saved in the current Spotify user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs for the albums. Maximum: 50 IDs.
  
  Example: (is-saved-tracks? {:ids \"0udZHhCi7p1YzMlvI4fXoK,3SF5puV5eb6bgRSxBeMOk9\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/tracks/contains"))

(def get-saved-albums
  "Get a list of the albums saved in the current Spotify user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional keys in the map is :limit, :offset and :market.
  :limit The maximum number of objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first object to return. Default: 0.
  :market An ISO 3166-1 alpha-2 country code or the string from_token. 
  
  Example: (get-saved-albums {:limit 1 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "me/albums"))

(def get-saved-tracks
  "Get a list of the songs saved in the current Spotify user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional keys in the map is :limit, :offset and :market.
  :limit The maximum number of objects to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first object to return. Default: 0.
  :market An ISO 3166-1 alpha-2 country code or the string from_token. 
  
  Example: (get-saved-tracks {:limit 1 :offset 5 :market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/tracks"))

(def remove-saved-albums
  "Remove one or more albums from the current user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs. Maximum 50.
  
  Example: (remove-saved-albums {:ids \"4iV5W9uYEdYUVa79Axb7Rh,1301WleyT98MSxVHPZCA6M\"} \"OAUTH-TOKEN\")"
  (partial delete-request "me/albums"))

(def remove-saved-tracks
  "Remove one or more tracks from the current user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs. Maximum 50.
  
  Example: (remove-saved-tracks {:ids \"4iV5W9uYEdYUVa79Axb7Rh,1301WleyT98MSxVHPZCA6M\"} \"OAUTH-TOKEN\")"
  (partial delete-request "me/tracks"))

(def save-albums
  "Save one or more albums to the current user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs. Maximum 50.

  Example: (save-albums {:ids \"4iV5W9uYEdYUVa79Axb7Rh,1301WleyT98MSxVHPZCA6M\"} \"OAUTH-TOKEN\")"
  (partial put-request "me/albums"))

(def save-tracks
  "Save one or more tracks to the current user’s ‘Your Music’ library.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs. Maximum 50.

  Example: (save-tracks {:ids \"4iV5W9uYEdYUVa79Axb7Rh,1301WleyT98MSxVHPZCA6M\"} \"OAUTH-TOKEN\")"
  (partial put-request "me/tracks"))

; Personalization API Endpoints
;TODO: Implement, api params doesn't support type yet.
(def get-users-top-artist-and-tracks
  ""
  (partial get-request "me/top/type"))

; Player API Endpoints
(def get-available-devices
  "Get information about a user’s available devices.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  There are no keys in the map.

  Example: (get-available-devices {} \"OAUTH-TOKEN\")"
  (partial get-request "me/player/devices"))

(def get-current-playback-info
  "Get information about the user’s current playback state, including track, track progress, and active device.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :market.
  :market An ISO 3166-1 alpha-2 country code or the string from_token.
  
  Example: (get-current-playback-info {:market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/player"))

(def get-recently-played-tracks
  "Get tracks from the current user’s recently played tracks.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional keys in the map is :limit, :after and :before.
  :limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
  :after A Unix timestamp in milliseconds.
  :before A Unix timestamp in milliseconds.
  
  Example: (get-recently-played-tracks {:limit 10 :after 1484811043508} \"OAUTH-TOKEN\")"
  (partial get-request "me/player/recently-played"))

(def get-currently-playing-track
  "Get the object currently being played on the user’s Spotify account.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :market.
  :market An ISO 3166-1 alpha-2 country code or the string from_token.
  
  Example: (get-currently-playing-track {:market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "me/player/currently-playing"))

(def pause-user-playback
  "Pause playback on the user’s account.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :device_id.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (pause-user-playback {:device_id \"0d1841b0976bae2a3a310dd74c0f3df354899bc8\"} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/pause"))

(def player-seek-to
  "Seeks to the given position in the user’s currently playing track.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :position_ms. Optional key is :device_id.
  :position_ms  The position in milliseconds to seek to.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.

  Example: (player-seek-to {:position_ms 25000} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/seek"))

(def player-toggle-repeat
  "Set the repeat mode for the user’s playback. Options are repeat-track, repeat-context, and off.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :state. Optional key is :device_id.
  :state track, context or off. track will repeat the current track. context will repeat the current context.
    off will turn repeat off.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.

  Example: (player-toggle-repeat {:state \"context\"} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/repeat"))

(def player-set-volume
  "Set the volume for the user’s current playback device.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key is :volume_percent. Optional key is :device_id.
  :volume_percent Integer. The volume to set. Must be a value from 0 to 100 inclusive.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (player-set-volume {:volume_percent 50} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/volume"))

(def player-next-track
  "Skips to next track in the user’s queue.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :device_id.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (player-next-track {} \"OAUTH-TOKEN\")"
  (partial post-request "me/player/next"))

(def player-previous-track
  "Skips to previous track in the user’s queue.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :device_id.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (player-previous-track {} \"OAUTH-TOKEN\")"
  (partial post-request "me/player/previous"))

(def player-start-resume-playback
  "Start a new context or resume current playback on the user’s active device.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional key in the map is :device_id.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (player-start-resume-playback {} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/play"))

(def player-toggle-shuffle
  "Toggle shuffle on or off for user’s playback.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :state. Optional key in the map is device_id.
  :state true: shuffle user's playback. false: Do not shuffle user's playback.
  :device_id The id of the device this command is targeting.
    If not supplied, the user’s currently active device is the target.
  
  Example: (player-toggle-shuffle {:state true} \"OAUTH-TOKEN\")"
  (partial put-request "me/player/shuffle"))

; TODO: Implement body parameters for endpoints.
(def player-transfer-playback
  ""
  (partial put-request "me/player"))

; Playlists API Endpoints
(def add-tracks-to-playlist
  "Add one or more tracks to a user’s playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id. Optional keys are :uris and :position.
  :playlist_id The Spotify ID for the playlist.
  :uris A comma-separated list of Spotify track URIs to add.
  :position The position to insert the tracks, a zero-based index. 
  
  Example: (add-tracks-to-playlist {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\" :position 0 :uris \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh,spotify:track:1301WleyT98MSxVHPZCA6M\"} \"OAUTH-TOKEN\")"
  (partial post-request "playlists/playlist_id/tracks"))

;TODO: Request body
(def change-playlist-details
  "Change a playlist’s name and public/private state. (The user must, of course, own the playlist.)
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id.
  :playlist_id The Spotify ID for the playlist.
  
  Example: (change-playlist-details {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\"} \"OAUTH-TOKEN\")"
  (partial put-request "playlists/playlist_id"))

;TODO: Request body
(def create-playlist
  "Create a playlist for a Spotify user. (The playlist will be empty until you add tracks.)
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :user_id.
  :user_id The user’s Spotify user ID.
  
  Example: (create-playlist {:user_id \"A-USER-ID\"} \"OAUTH-TOKEN\")"
  (partial post-request "users/user_id/playlists"))

(def get-playlists
  "Get a list of the playlists owned or followed by the current Spotify user.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Optional keys in the map is :limit and :offset.
  :limit The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first playlist to return. Default: 0
  
  Example: (get-playlists {:limit 20 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "me/playlists"))

(def get-users-playlists
  "Get a list of the playlists owned or followed by a Spotify user.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :user_id. Optional keys are :limit and :offset.
  :user_id The user’s Spotify user ID.
  :limit The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
  :offset The index of the first playlist to return. Default: 0
  
  Example: (get-users-playlists {:user_id \"AN-USER-ID\" :limit 20 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "users/user_id/playlists"))

(def get-playlist-cover-image
  "Get the current image associated with a specific playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id.
  :playlist_id The Spotify ID for the playlist.
  
  Example: (get-playlist-cover-image {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\"} \"OAUTH-TOKEN\")"
  (partial get-request "playlists/playlist_id/images"))

(def get-playlist
  "Get a playlist owned by a Spotify user.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id. Optional keys are :fields and :market
  :playlist_id The Spotify ID for the playlist.
  :fields Filters for the query: a comma-separated list of the fields to return.
  :market An ISO 3166-1 alpha-2 country code or the string from_token. 
  
  Example: (get-playlist {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\" :fields \"items(added_by.id,track(name,href,album(name,href)))\" :market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "playlists/playlist_id"))

(def get-playlist-tracks
  "Get full details of the tracks of a playlist owned by a Spotify user.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id. Optional keys are :fields, :market, :limit and :offset
  :playlist_id The Spotify ID for the playlist.
  :fields Filters for the query: a comma-separated list of the fields to return.
  :market An ISO 3166-1 alpha-2 country code or the string from_token. 
  :limit The maximum number of tracks to return. Default: 100. Minimum: 1. Maximum: 100.
  :offset The index of the first track to return. Default: 0 (the first object).
  
  Example: (get-playlist-tracks {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\" :fields \"items(added_by.id,track(name,href,album(name,href)))\" :market \"ES\" :limit 50 :offset 5} \"OAUTH-TOKEN\")"
  (partial get-request "playlists/playlist_id/tracks"))

;TODO: Request body
(def remove-tracks-from-playlist
  "Remove one or more tracks from a user’s playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id and :tracks.
  :playlist_id The Spotify ID for the playlist.
  :tracks An array of objects containing Spotify URIs of the tracks to remove.
  
  Example: (remove-tracks-from-playlist {} \"OAUTH-TOKEN\")"
  (partial delete-request "playlists/playlist_id/tracks"))

;TODO: Request body
(def reorder-playlist-tracks
  ""
  (partial put-request "playlists/playlist_id/tracks"))

(def replace-playlist-tracks
  "Replace all the tracks in a playlist, overwriting its existing tracks.
  This powerful request can be useful for replacing tracks,
  re-ordering existing tracks, or clearing the playlist.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :playlist_id. Optional key in the map is :uris.
  :playlist_id The Spotify ID for the playlist.
  :uris  A comma-separated list of Spotify track URIs to set. 
  
  Example: (replace-playlist-tracks {:playlist_id \"3cEYpjA9oz9GiPac4AsH4n\"} \"OAUTH-TOKEN\")"
  (partial put-request "playlists/playlist_id/tracks"))

;TODO: Request body
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
  "Get a detailed audio analysis for a single track identified by its unique Spotify ID.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :id.
  :id The Spotify ID for the track.

  Example: (get-audio-analysis {:id \"06AKEBrKUckW0KREUWRnvT\"} \"OAUTH-TOKEN\")"
  (partial get-request "audio-analysis/id"))

(def get-track-audio-features
  "Get audio feature information for a single track identified by its unique Spotify ID.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :id.
  :id The Spotify ID for the track.

  Example: (get-track-audio-features {:id \"06AKEBrKUckW0KREUWRnvT\"} \"OAUTH-TOKEN\")"
  (partial get-request "audio-features/id"))

(def get-tracks-audio-features
  "Get audio features for multiple tracks based on their Spotify IDs.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids.
  :ids A comma-separated list of the Spotify IDs for the tracks. Maximum: 100 IDs.
  
  Example: (get-tracks-audio-features {:ids \"4JpKVNYnVcJ8tuMKjAj50A,2NRANZE9UCmPAS5XVbXL40\"} \"OAUTH-TOKEN\")"
  (partial get-request "audio-features/"))

(def get-tracks
  "Get Spotify catalog information for multiple tracks based on their Spotify IDs.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :ids. Optional key is :market
  :ids A comma-separated list of the Spotify IDs for the tracks. Maximum: 50 IDs.
  :market An ISO 3166-1 alpha-2 country code or the string from_token.
  
  Example: (get-tracks {:ids \"3n3Ppam7vgaVa1iaRUc9Lp,3twNvmDtFQtAd5gMKedhLD\" :market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "tracks/"))

(def get-track
  "Get Spotify catalog information for a single track identified by its unique Spotify ID.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :id. Optional key in the map is :market.
  :id The Spotify ID for the track.
  :market  An ISO 3166-1 alpha-2 country code or the string from_token. 
  
  Example: (get-track {:id \"3n3Ppam7vgaVa1iaRUc9Lp\" :market \"ES\"} \"OAUTH-TOKEN\")"
  (partial get-request "tracks/id"))

; Users Profile API Endpoints
(def get-current-user-profile
  "Get detailed profile information about the current user (including the current user’s username).
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  
  Example: (get-current-user-profile {} \"OAUTH-TOKEN\")"
  (partial get-request "me/"))

(def get-user-profile
  "Get public profile information about a Spotify user.
  Takes two arguments, a map with the path and query parameters and a oauth-token.
  Required key in the map is :user_id.
  :user_id The user’s Spotify user ID.
  
  Example: (get-user-profile {:user_id \"wizzler\"} \"OAUTH-TOKEN\")"
  (partial get-request "users/user_id"))
