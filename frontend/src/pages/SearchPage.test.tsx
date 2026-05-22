import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import type { TrackPreview } from '../services/api'

const { mockCheckAuth, mockPreviewPlaylist, mockNavigate, mockSearchArtists } = vi.hoisted(() => ({
  mockCheckAuth: vi.fn(),
  mockPreviewPlaylist: vi.fn(),
  mockNavigate: vi.fn(),
  mockSearchArtists: vi.fn(),
}))

vi.mock('../services/api', () => ({
  spotifyApi: {
    previewPlaylist: mockPreviewPlaylist,
    checkAuth: mockCheckAuth,
    searchArtists: mockSearchArtists,
  },
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

import { SearchPage } from './SearchPage'

const MOCK_PREVIEWS: TrackPreview[] = [
  {
    setlistTrack: 'Yesterday',
    plays: 150,
    coverArtist: null,
    matched: true,
    spotifyTrackId: 'id-yesterday',
    spotifyTrackName: 'Yesterday',
    spotifyArtistName: 'The Beatles',
    confidence: 95,
    reason: 'EXACT_TRACK',
    alternatives: [],
  },
  {
    setlistTrack: 'Hey Jude',
    plays: 120,
    coverArtist: null,
    matched: true,
    spotifyTrackId: 'id-hey-jude',
    spotifyTrackName: 'Hey Jude',
    spotifyArtistName: 'The Beatles',
    confidence: 90,
    reason: 'EXACT_TRACK',
    alternatives: [],
  },
]

describe('SearchPage', () => {
  beforeEach(() => {
    mockCheckAuth.mockClear()
    mockCheckAuth.mockResolvedValue({ data: { authenticated: true } } as any)
    mockPreviewPlaylist.mockClear()
    mockNavigate.mockClear()
    mockSearchArtists.mockClear()
    mockSearchArtists.mockResolvedValue({ data: [] } as any)
  })

  const renderSearchPage = () => {
    return render(
      <BrowserRouter>
        <SearchPage />
      </BrowserRouter>
    )
  }

  describe('rendering', () => {
    it('should render the search page with title', () => {
      renderSearchPage()
      expect(screen.getByText('Concert Playlist Builder')).toBeInTheDocument()
    })

    it('should render the artist search input', async () => {
      renderSearchPage()
      expect(await screen.findByPlaceholderText('Search for an artist...')).toBeInTheDocument()
    })

    it('should render the search button', async () => {
      renderSearchPage()
      expect(await screen.findByRole('button', { name: /Search/i })).toBeInTheDocument()
    })

    it('should render the description text', () => {
      renderSearchPage()
      expect(screen.getByText('Create playlists from concert setlists')).toBeInTheDocument()
    })
  })

  describe('authentication state', () => {
    it('should show login button when not authenticated', async () => {
      mockCheckAuth.mockResolvedValue({ data: { authenticated: false } } as any)
      renderSearchPage()
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Login with Spotify/i })).toBeInTheDocument()
      })
    })

    it('should show logout button when authenticated', async () => {
      mockCheckAuth.mockResolvedValue({ data: { authenticated: true } } as any)
      renderSearchPage()
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Logout/i })).toBeInTheDocument()
      })
    })

    it('should display logged in indicator when authenticated', async () => {
      mockCheckAuth.mockResolvedValue({ data: { authenticated: true } } as any)
      renderSearchPage()
      await waitFor(() => {
        expect(screen.getByText(/Logged in with Spotify/i)).toBeInTheDocument()
      })
    })

    it('should call checkAuth on component mount', async () => {
      mockCheckAuth.mockResolvedValue({ data: { authenticated: false } } as any)
      renderSearchPage()
      await waitFor(() => {
        expect(mockCheckAuth).toHaveBeenCalledTimes(1)
      })
    })
  })

  describe('search functionality', () => {
    it('should disable search button when input is empty', async () => {
      renderSearchPage()
      const searchButton = await screen.findByRole('button', { name: /Search/i })
      expect(searchButton).toBeDisabled()
    })

    it('should enable search button when input has text', async () => {
      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      await userEvent.type(input, 'Beatles')
      expect(screen.getByRole('button', { name: /Search/i })).not.toBeDisabled()
    })

    it('should navigate to results page on successful search', async () => {
      mockPreviewPlaylist.mockResolvedValue({ data: MOCK_PREVIEWS } as any)

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      const searchButton = await screen.findByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Beatles')
      await userEvent.click(searchButton)

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/results', {
          state: { artist: 'Beatles', previews: MOCK_PREVIEWS },
        })
      })
    })

    it('should display error message on search failure', async () => {
      mockPreviewPlaylist.mockRejectedValue({
        response: { data: { message: 'Artist not found' } },
      })

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      const searchButton = await screen.findByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Unknown Artist')
      await userEvent.click(searchButton)

      await waitFor(() => {
        expect(screen.getByText('Artist not found')).toBeInTheDocument()
      })
    })

    it('should show loading state while searching', async () => {
      mockPreviewPlaylist.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: MOCK_PREVIEWS } as any), 100))
      )

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      const searchButton = await screen.findByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Beatles')
      await userEvent.click(searchButton)

      expect(screen.getByRole('button', { name: /Searching/i })).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /Searching/i })).not.toBeInTheDocument()
      })
    })

    it('should show error when no tracks are found', async () => {
      mockPreviewPlaylist.mockResolvedValue({ data: [] } as any)

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      await userEvent.type(input, 'Obscure Artist')
      await userEvent.click(screen.getByRole('button', { name: /Search/i }))

      await waitFor(() => {
        expect(screen.getByText('No concert data found for this artist')).toBeInTheDocument()
      })
    })
  })

  describe('autocomplete', () => {
    it('should show suggestions dropdown when typing', async () => {
      mockSearchArtists.mockResolvedValue({
        data: [{ id: '1', name: 'The Beatles', imageUrl: null }],
      } as any)

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      await userEvent.type(input, 'Beat')

      await waitFor(() => {
        expect(screen.getByText('The Beatles')).toBeInTheDocument()
      })
    })

    it('should populate input when a suggestion is clicked', async () => {
      mockSearchArtists.mockResolvedValue({
        data: [{ id: '1', name: 'The Beatles', imageUrl: null }],
      } as any)

      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      await userEvent.type(input, 'Beat')

      await waitFor(() => screen.getByText('The Beatles'))
      await userEvent.click(screen.getByText('The Beatles'))

      expect(input).toHaveValue('The Beatles')
    })

    it('should not show suggestions when query is less than 2 characters', async () => {
      renderSearchPage()
      const input = await screen.findByPlaceholderText('Search for an artist...')
      await userEvent.type(input, 'B')
      expect(mockSearchArtists).not.toHaveBeenCalled()
    })
  })

  describe('login/logout', () => {
    it('should redirect to Spotify login on login button click', async () => {
      const { spotifyApi } = await import('../services/api')
      vi.mocked(spotifyApi.checkAuth).mockResolvedValue({
        data: { authenticated: false },
      } as any)

      const locationSpy = vi.spyOn(window, 'location', 'get')
      renderSearchPage()

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Login with Spotify/i })).toBeInTheDocument()
      })

      locationSpy.mockRestore()
    })

    it('should handle logout', async () => {
      const { spotifyApi } = await import('../services/api')
      vi.mocked(spotifyApi.checkAuth).mockResolvedValue({
        data: { authenticated: true },
      } as any)

      renderSearchPage()

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Logout/i })).toBeInTheDocument()
      })
    })
  })
})
