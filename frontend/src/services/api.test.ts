import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import { TrackCountResponse, PlaylistResponse } from './api'

// Mock axios
vi.mock('axios')
const mockedAxios = axios as unknown as {
  create: ReturnType<typeof vi.fn>
}

describe('spotifyApi', () => {
  let mockApiClient: any

  beforeEach(() => {
    // Create a mock API client with all methods
    mockApiClient = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    }

    // Mock axios.create to return our mock client
    vi.mocked(mockedAxios.create).mockReturnValue(mockApiClient)

    // Re-import spotifyApi to get the mocked client
    vi.resetModules()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getTopTracks', () => {
    it('should fetch top tracks for an artist', async () => {
      const artistName = 'The Beatles'
      const mockTracks: TrackCountResponse[] = [
        { track: 'Yesterday', plays: 150 },
        { track: 'Hey Jude', plays: 120 },
      ]

      mockApiClient.get.mockResolvedValue({
        data: { trackCounts: mockTracks },
      })

      // Re-import after mock setup
      const { spotifyApi: api } = await import('./api')
      const response = await api.getTopTracks(artistName)

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/setlist/${artistName}/top-tracks`
      )
      expect(response.data.trackCounts).toEqual(mockTracks)
    })

    it('should handle artist names with special characters', async () => {
      const artistName = 'AC/DC'
      mockApiClient.get.mockResolvedValue({
        data: { trackCounts: [] },
      })

      const { spotifyApi: api } = await import('./api')
      await api.getTopTracks(artistName)

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/setlist/${artistName}/top-tracks`
      )
    })

    it('should handle network errors gracefully', async () => {
      const artistName = 'Unknown Artist'
      const errorMessage = 'Network Error'

      mockApiClient.get.mockRejectedValue(
        new Error(errorMessage)
      )

      const { spotifyApi: api } = await import('./api')

      await expect(api.getTopTracks(artistName)).rejects.toThrow(errorMessage)
    })

    it('should handle empty track results', async () => {
      const artistName = 'No Concert Data'
      mockApiClient.get.mockResolvedValue({
        data: { trackCounts: [] },
      })

      const { spotifyApi: api } = await import('./api')
      const response = await api.getTopTracks(artistName)

      expect(response.data.trackCounts).toEqual([])
    })
  })

  describe('createPlaylist', () => {
    it('should create a playlist with default limit', async () => {
      const artistName = 'Pink Floyd'
      const mockPlaylist: PlaylistResponse = {
        playlistId: 'spotify:playlist:123',
        playlistUrl: 'https://open.spotify.com/playlist/123',
        artist: artistName,
        tracksAdded: 20,
        topTracks: [
          { name: 'Wish You Were Here', confidence: 0.95 },
        ],
        createdAt: '2026-04-24T10:30:00Z',
      }

      mockApiClient.get.mockResolvedValue({ data: mockPlaylist })

      const { spotifyApi: api } = await import('./api')
      const response = await api.createPlaylist(artistName)

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/playlist/${artistName}/spotify-playlist`,
        { params: { limit: 20 } }
      )
      expect(response.data).toEqual(mockPlaylist)
    })

    it('should create a playlist with custom limit', async () => {
      const artistName = 'Queen'
      const limit = 50
      const mockPlaylist: PlaylistResponse = {
        playlistId: 'spotify:playlist:456',
        playlistUrl: 'https://open.spotify.com/playlist/456',
        artist: artistName,
        tracksAdded: 50,
        topTracks: [],
        createdAt: '2026-04-24T10:30:00Z',
      }

      mockApiClient.get.mockResolvedValue({ data: mockPlaylist })

      const { spotifyApi: api } = await import('./api')
      await api.createPlaylist(artistName, limit)

      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/playlist/${artistName}/spotify-playlist`,
        { params: { limit } }
      )
    })

    it('should include all required fields in response', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          playlistId: 'spotify:playlist:xyz',
          playlistUrl: 'https://open.spotify.com/playlist/xyz',
          artist: 'Test Artist',
          tracksAdded: 10,
          topTracks: [{ name: 'Track 1', confidence: 0.8 }],
          createdAt: '2026-04-24T00:00:00Z',
        },
      })

      const { spotifyApi: api } = await import('./api')
      const response = await api.createPlaylist('Test Artist')

      expect(response.data).toHaveProperty('playlistId')
      expect(response.data).toHaveProperty('playlistUrl')
      expect(response.data).toHaveProperty('artist')
      expect(response.data).toHaveProperty('tracksAdded')
      expect(response.data).toHaveProperty('topTracks')
      expect(response.data).toHaveProperty('createdAt')
    })

    it('should handle playlist creation errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 400,
          data: { message: 'Artist not found' },
        },
      })

      const { spotifyApi: api } = await import('./api')

      await expect(api.createPlaylist('Unknown')).rejects.toMatchObject({
        response: {
          status: 400,
        },
      })
    })
  })

  describe('checkAuth', () => {
    it('should check authentication status', async () => {
      mockApiClient.get.mockResolvedValue({
        data: { authenticated: true },
      })

      const { spotifyApi: api } = await import('./api')
      const response = await api.checkAuth()

      expect(mockApiClient.get).toHaveBeenCalledWith('/auth/check')
      expect(response.data.authenticated).toBe(true)
    })

    it('should return false when not authenticated', async () => {
      mockApiClient.get.mockResolvedValue({
        data: { authenticated: false },
      })

      const { spotifyApi: api } = await import('./api')
      const response = await api.checkAuth()

      expect(response.data.authenticated).toBe(false)
    })

    it('should handle auth check failures', async () => {
      mockApiClient.get.mockRejectedValue(
        new Error('Auth check failed')
      )

      const { spotifyApi: api } = await import('./api')

      await expect(api.checkAuth()).rejects.toThrow('Auth check failed')
    })
  })

  describe('loginUrl', () => {
    it('should return correct login URL', async () => {
      const { spotifyApi: api } = await import('./api')
      const url = api.loginUrl()

      expect(url).toBe('/api/../oauth2/authorization/spotify')
    })
  })

  describe('getMe', () => {
    it('should fetch current user info', async () => {
      const mockUser = {
        id: 'user123',
        display_name: 'Test User',
        email: 'test@example.com',
      }

      mockApiClient.get.mockResolvedValue({
        data: mockUser,
      })

      const { spotifyApi: api } = await import('./api')
      const response = await api.getMe()

      expect(mockApiClient.get).toHaveBeenCalledWith('/spotify/me')
      expect(response.data).toEqual(mockUser)
    })

    it('should handle getMe errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 401,
          data: { message: 'Unauthorized' },
        },
      })

      const { spotifyApi: api } = await import('./api')

      await expect(api.getMe()).rejects.toMatchObject({
        response: {
          status: 401,
        },
      })
    })
  })
})
