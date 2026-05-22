import axios from 'axios'

const API_BASE = '/api'

const apiClient = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
})

export interface TopTrack {
  name: string
  confidence: number
}

export interface PlaylistResponse {
  playlistId: string
  playlistUrl: string
  artist: string
  tracksAdded: number
  topTracks: TopTrack[]
  createdAt: string
}

export interface TrackCountResponse {
  track: string
  plays: number
  coverArtist?: string | null
}

export interface TrackCountsResponse {
  trackCounts: TrackCountResponse[]
}

export interface ArtistSuggestion {
  id: string
  name: string
  imageUrl: string | null
}

/** One runner-up track shown in the swap picker. */
export interface AlternativeTrack {
  spotifyTrackId: string
  spotifyTrackName: string
  spotifyArtistName: string | null
  confidence: number
  albumImageUrl?: string | null
}

/** Full resolution preview for a single setlist.fm track. */
export interface TrackPreview {
  setlistTrack: string
  plays: number
  coverArtist: string | null
  matched: boolean
  spotifyTrackId: string | null
  spotifyTrackName: string | null
  spotifyArtistName: string | null
  confidence: number
  reason: string
  alternatives: AlternativeTrack[]
  albumImageUrl?: string | null
}

export const spotifyApi = {
  loginUrl: () => `${API_BASE}/../oauth2/authorization/spotify`,

  getMe: () => apiClient.get('/spotify/me'),

  getTopTracks: (artist: string) =>
    apiClient.get<TrackCountsResponse>(`/setlist/${artist}/top-tracks`),

  /**
   * Phase 1 of the new two-phase create flow.
   * Resolves all top tracks for the artist against Spotify and returns the full
   * preview (best match + swap alternatives) without creating a playlist.
   */
  previewPlaylist: (artist: string, limit = 20) =>
    apiClient.get<TrackPreview[]>(`/playlist/${artist}/preview`, {
      params: { limit },
    }),

  /**
   * Phase 2 of the new two-phase create flow.
   * Creates a Spotify playlist from a pre-confirmed list of Spotify track IDs.
   * The caller has already resolved, reviewed, and possibly swapped tracks in Phase 1.
   */
  createPlaylistFromTracks: (artist: string, trackIds: string[]) =>
    apiClient.post<PlaylistResponse>(`/playlist/${artist}/create-from-tracks`, {
      trackIds,
    }),

  /** Legacy single-step create (kept for backward compat). */
  createPlaylist: (artist: string, limit?: number) =>
    apiClient.get<PlaylistResponse>(`/playlist/${artist}/spotify-playlist`, {
      params: { limit: limit || 20 },
    }),

  checkAuth: () => apiClient.get('/auth/check'),

  searchArtists: (query: string, signal?: AbortSignal) =>
    apiClient.get<ArtistSuggestion[]>('/artists/search', { params: { q: query }, signal }),
}

export default apiClient
