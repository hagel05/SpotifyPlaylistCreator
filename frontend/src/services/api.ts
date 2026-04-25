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
}

export interface TrackCountsResponse {
  trackCounts: TrackCountResponse[]
}

export const spotifyApi = {
  loginUrl: () => `${API_BASE}/../oauth2/authorization/spotify`,

  getMe: () => apiClient.get('/spotify/me'),

  getTopTracks: (artist: string) =>
    apiClient.get<TrackCountsResponse>(`/setlist/${artist}/top-tracks`),

  createPlaylist: (artist: string, limit?: number) =>
    apiClient.get<PlaylistResponse>(`/playlist/${artist}/spotify-playlist`, {
      params: { limit: limit || 20 },
    }),

  checkAuth: () => apiClient.get('/auth/check'),
}

export default apiClient
