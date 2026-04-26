import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'

// Use vi.hoisted to properly hoist mock definitions
const { mockCheckAuth, mockGetTopTracks, mockNavigate } = vi.hoisted(() => ({
  mockCheckAuth: vi.fn(),
  mockGetTopTracks: vi.fn(),
  mockNavigate: vi.fn(),
}))

vi.mock('../services/api', () => ({
  spotifyApi: {
    getTopTracks: mockGetTopTracks,
    checkAuth: mockCheckAuth,
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

describe('SearchPage', () => {
  beforeEach(() => {
    mockCheckAuth.mockClear()
    mockGetTopTracks.mockClear()
    mockNavigate.mockClear()
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

    it('should render the artist search input', () => {
      renderSearchPage()
      expect(screen.getByPlaceholderText('Search for an artist...')).toBeInTheDocument()
    })

    it('should render the search button', () => {
      renderSearchPage()
      expect(screen.getByRole('button', { name: /Search/i })).toBeInTheDocument()
    })

    it('should render the description text', () => {
      renderSearchPage()
      expect(screen.getByText('Create playlists from concert setlists')).toBeInTheDocument()
    })
  })

  describe('authentication state', () => {
    it('should show login button when not authenticated', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: false },
      } as any)

      renderSearchPage()

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Login with Spotify/i })).toBeInTheDocument()
      })
    })

    it('should show logout button when authenticated', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      renderSearchPage()

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Logout/i })).toBeInTheDocument()
      })
    })

    it('should display logged in indicator when authenticated', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      renderSearchPage()

      await waitFor(() => {
        expect(screen.getByText(/Logged in with Spotify/i)).toBeInTheDocument()
      })
    })

    it('should call checkAuth on component mount', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: false },
      } as any)

      renderSearchPage()

      await waitFor(() => {
        expect(mockCheckAuth).toHaveBeenCalledTimes(1)
      })
    })
  })

  describe('search functionality', () => {
    it('should disable search button when input is empty', () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      renderSearchPage()

      const searchButton = screen.getByRole('button', { name: /Search/i })
      expect(searchButton).toBeDisabled()
    })

    it('should enable search button when input has text', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      renderSearchPage()
      const input = screen.getByPlaceholderText('Search for an artist...')

      await userEvent.type(input, 'Beatles')

      const searchButton = screen.getByRole('button', { name: /Search/i })
      expect(searchButton).not.toBeDisabled()
    })

    it('should navigate to results page on successful search', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      const mockTracks = [
        { track: 'Yesterday', plays: 150 },
        { track: 'Hey Jude', plays: 120 },
      ]

      mockGetTopTracks.mockResolvedValue({
        data: { trackCounts: mockTracks },
      } as any)

      renderSearchPage()
      const input = screen.getByPlaceholderText('Search for an artist...')
      const searchButton = screen.getByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Beatles')
      await userEvent.click(searchButton)

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/results', {
          state: {
            artist: 'Beatles',
            topTracks: mockTracks,
          },
        })
      })
    })

    it('should display error message on search failure', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      mockGetTopTracks.mockRejectedValue({
        response: {
          data: { message: 'Artist not found' },
        },
      })

      renderSearchPage()
      const input = screen.getByPlaceholderText('Search for an artist...')
      const searchButton = screen.getByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Unknown Artist')
      await userEvent.click(searchButton)

      await waitFor(() => {
        expect(screen.getByText('Artist not found')).toBeInTheDocument()
      })
    })

    it('should show loading state while searching', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { authenticated: true },
      } as any)

      mockGetTopTracks.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: { trackCounts: [] } } as any), 100))
      )

      renderSearchPage()
      const input = screen.getByPlaceholderText('Search for an artist...')
      const searchButton = screen.getByRole('button', { name: /Search/i })

      await userEvent.type(input, 'Beatles')
      await userEvent.click(searchButton)

      expect(screen.getByRole('button', { name: /Searching/i })).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /Searching/i })).not.toBeInTheDocument()
      })
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
        const loginButton = screen.getByRole('button', { name: /Login with Spotify/i })
        expect(loginButton).toBeInTheDocument()
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
        const logoutButton = screen.getByRole('button', { name: /Logout/i })
        expect(logoutButton).toBeInTheDocument()
      })
    })
  })
})
