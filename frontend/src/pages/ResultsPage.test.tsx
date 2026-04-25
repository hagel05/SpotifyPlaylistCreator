import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'

// Use vi.hoisted to properly hoist mock definitions
const { mockCreatePlaylist, mockNavigate, getMockLocationState, setMockLocationState } = vi.hoisted(() => {
  let mockLocationState = {
    artist: 'The Beatles',
    topTracks: [
      { track: 'Yesterday', plays: 150 },
      { track: 'Hey Jude', plays: 120 },
      { track: 'Let It Be', plays: 100 },
    ],
  }

  return {
    mockCreatePlaylist: vi.fn(),
    mockNavigate: vi.fn(),
    getMockLocationState: () => mockLocationState,
    setMockLocationState: (state: any) => {
      mockLocationState = state
    },
  }
})

vi.mock('../services/api', () => ({
  spotifyApi: {
    createPlaylist: mockCreatePlaylist,
  },
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({
      state: getMockLocationState(),
    }),
  }
})

import { ResultsPage } from './ResultsPage'

describe('ResultsPage', () => {
  beforeEach(() => {
    mockCreatePlaylist.mockClear()
    mockNavigate.mockClear()
    setMockLocationState({
      artist: 'The Beatles',
      topTracks: [
        { track: 'Yesterday', plays: 150 },
        { track: 'Hey Jude', plays: 120 },
        { track: 'Let It Be', plays: 100 },
      ],
    })
  })

  const renderResultsPage = () => {
    return render(
      <BrowserRouter>
        <ResultsPage />
      </BrowserRouter>
    )
  }

  describe('rendering with valid data', () => {
    it('should render artist name', () => {
      renderResultsPage()
      expect(screen.getByText('The Beatles')).toBeInTheDocument()
    })

    it('should display track count', () => {
      renderResultsPage()
      expect(screen.getByText(/Top 3 tracks from concert setlists/)).toBeInTheDocument()
    })

    it('should render all tracks with their play counts', () => {
      renderResultsPage()

      expect(screen.getByText('Yesterday')).toBeInTheDocument()
      expect(screen.getByText('150 plays')).toBeInTheDocument()

      expect(screen.getByText('Hey Jude')).toBeInTheDocument()
      expect(screen.getByText('120 plays')).toBeInTheDocument()

      expect(screen.getByText('Let It Be')).toBeInTheDocument()
      expect(screen.getByText('100 plays')).toBeInTheDocument()
    })

    it('should render back to search button', () => {
      renderResultsPage()
      expect(screen.getByRole('button', { name: /Back to Search/i })).toBeInTheDocument()
    })

    it('should render create playlist button', () => {
      renderResultsPage()
      expect(screen.getByRole('button', { name: /Create Spotify Playlist/i })).toBeInTheDocument()
    })

    it('should display preview message', () => {
      renderResultsPage()
      expect(screen.getByText(/Preview these tracks before creating your playlist/)).toBeInTheDocument()
    })
  })

  describe('no artist state', () => {
    it('should render no results message when artist is missing', () => {
      setMockLocationState({ artist: null, topTracks: [] })

      render(
        <BrowserRouter>
          <ResultsPage />
        </BrowserRouter>
      )

      expect(screen.getByText('No results to display. Please search for an artist.')).toBeInTheDocument()
    })

    it('should show back to search button on no artist state', () => {
      setMockLocationState({ artist: null, topTracks: [] })

      render(
        <BrowserRouter>
          <ResultsPage />
        </BrowserRouter>
      )

      expect(screen.getByRole('button', { name: /Back to Search/i })).toBeInTheDocument()
    })
  })

  describe('no tracks state', () => {
    it('should render no tracks message when tracks array is empty', () => {
      setMockLocationState({ artist: 'Unknown Artist', topTracks: [] })

      render(
        <BrowserRouter>
          <ResultsPage />
        </BrowserRouter>
      )

      expect(screen.getByText(/No tracks found for "Unknown Artist"/)).toBeInTheDocument()
    })

    it('should show informative message about missing concert data', () => {
      setMockLocationState({ artist: 'Obscure Band', topTracks: [] })

      render(
        <BrowserRouter>
          <ResultsPage />
        </BrowserRouter>
      )

      expect(screen.getByText(/may not have concert data or may not be available/)).toBeInTheDocument()
    })
  })

  describe('playlist creation', () => {
    it('should create playlist on button click', async () => {
      const mockPlaylist = {
        playlistId: 'spotify:playlist:123',
        playlistUrl: 'https://open.spotify.com/playlist/123',
        artist: 'The Beatles',
        tracksAdded: 3,
        topTracks: [],
        createdAt: '2026-04-24T10:30:00Z',
      }

      mockCreatePlaylist.mockResolvedValue({
        data: mockPlaylist,
      } as any)

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      await waitFor(() => {
        expect(mockCreatePlaylist).toHaveBeenCalledWith('The Beatles', 20)
      })
    })

    it('should navigate to playlist page on successful creation', async () => {
      const mockPlaylist = {
        playlistId: 'spotify:playlist:123',
        playlistUrl: 'https://open.spotify.com/playlist/123',
        artist: 'The Beatles',
        tracksAdded: 3,
        topTracks: [{ name: 'Yesterday', confidence: 0.95 }],
        createdAt: '2026-04-24T10:30:00Z',
      }

      mockCreatePlaylist.mockResolvedValue({
        data: mockPlaylist,
      } as any)

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/playlist', {
          state: {
            playlist: mockPlaylist,
          },
        })
      })
    })

    it('should show loading state while creating playlist', async () => {
      mockCreatePlaylist.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: {} } as any), 100))
      )

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      expect(screen.getByRole('button', { name: /Creating Playlist/i })).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /Creating Playlist/i })).not.toBeInTheDocument()
      })
    })

    it('should disable button while creating playlist', async () => {
      mockCreatePlaylist.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: {} } as any), 100))
      )

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      expect(createButton).toBeDisabled()

      await waitFor(() => {
        expect(createButton).not.toBeDisabled()
      })
    })

    it('should display error message on creation failure', async () => {
      mockCreatePlaylist.mockRejectedValue({
        response: {
          data: { message: 'Spotify API rate limit exceeded' },
        },
      })

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      await waitFor(() => {
        expect(screen.getByText('Spotify API rate limit exceeded')).toBeInTheDocument()
      })
    })

    it('should display generic error message when no specific error provided', async () => {
      mockCreatePlaylist.mockRejectedValue({
        message: 'Unknown error',
      })

      renderResultsPage()
      const createButton = screen.getByRole('button', { name: /Create Spotify Playlist/i })

      await userEvent.click(createButton)

      await waitFor(() => {
        expect(screen.getByText('Failed to create playlist')).toBeInTheDocument()
      })
    })
  })

  describe('navigation', () => {
    it('should navigate back to search when back button is clicked', async () => {
      renderResultsPage()
      const backButton = screen.getByRole('button', { name: /Back to Search/i })

      await userEvent.click(backButton)

      expect(mockNavigate).toHaveBeenCalledWith('/')
    })
  })
})
