import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'

const {
  mockCreatePlaylistFromTracks,
  mockNavigate,
  getMockLocationState,
  setMockLocationState,
} = vi.hoisted(() => {
  let mockLocationState: any = {}

  return {
    mockCreatePlaylistFromTracks: vi.fn(),
    mockNavigate: vi.fn(),
    getMockLocationState: () => mockLocationState,
    setMockLocationState: (state: any) => { mockLocationState = state },
  }
})

vi.mock('../services/api', () => ({
  spotifyApi: {
    createPlaylistFromTracks: mockCreatePlaylistFromTracks,
  },
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: getMockLocationState() }),
  }
})

import { ResultsPage } from './ResultsPage'
import type { TrackPreview } from '../services/api'

// ── Helpers ───────────────────────────────────────────────────────────────────

const makePreview = (overrides: Partial<TrackPreview> & { setlistTrack: string; plays: number }): TrackPreview => ({
  coverArtist: null,
  matched: true,
  spotifyTrackId: `id-${overrides.setlistTrack}`,
  spotifyTrackName: overrides.setlistTrack,
  spotifyArtistName: 'The Beatles',
  confidence: 85,
  reason: 'EXACT_TRACK',
  alternatives: [],
  ...overrides,
})

const DEFAULT_PREVIEWS: TrackPreview[] = [
  makePreview({ setlistTrack: 'Yesterday', plays: 150 }),
  makePreview({ setlistTrack: 'Hey Jude', plays: 120 }),
  makePreview({ setlistTrack: 'Let It Be', plays: 100 }),
]

function renderResultsPage() {
  return render(
    <BrowserRouter>
      <ResultsPage />
    </BrowserRouter>
  )
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ResultsPage', () => {
  beforeEach(() => {
    mockCreatePlaylistFromTracks.mockClear()
    mockNavigate.mockClear()
    setMockLocationState({
      artist: 'The Beatles',
      previews: DEFAULT_PREVIEWS,
    })
  })

  describe('rendering with valid data', () => {
    it('should render artist name', () => {
      renderResultsPage()
      expect(screen.getByText('The Beatles')).toBeInTheDocument()
    })

    it('should display track count', () => {
      renderResultsPage()
      expect(screen.getByText(/3 tracks from concert setlists/)).toBeInTheDocument()
    })

    it('should render all tracks with their play counts', () => {
      renderResultsPage()

      expect(screen.getAllByText('Yesterday')[0]).toBeInTheDocument()
      expect(screen.getByText('150 plays')).toBeInTheDocument()
      expect(screen.getAllByText('Hey Jude')[0]).toBeInTheDocument()
      expect(screen.getByText('120 plays')).toBeInTheDocument()
      expect(screen.getAllByText('Let It Be')[0]).toBeInTheDocument()
      expect(screen.getByText('100 plays')).toBeInTheDocument()
    })

    it('should display tracks in ascending play-count order (lowest first, highest last)', () => {
      setMockLocationState({
        artist: 'The Beatles',
        previews: [
          makePreview({ setlistTrack: 'Yesterday', plays: 150 }),
          makePreview({ setlistTrack: 'Hey Jude', plays: 120 }),
          makePreview({ setlistTrack: 'Let It Be', plays: 100 }),
        ],
      })

      renderResultsPage()

      const trackNames = screen
        .getAllByTestId('track-item')
        .map(el => el.querySelector('.font-medium')?.textContent)

      // Lowest plays (100) first, highest plays (150) last — mirrors playlist order
      expect(trackNames).toEqual(['Let It Be', 'Hey Jude', 'Yesterday'])
    })

    it('should render back to search button', () => {
      renderResultsPage()
      expect(screen.getByRole('button', { name: /Back to Search/i })).toBeInTheDocument()
    })

    it('should render create playlist button with track count', () => {
      renderResultsPage()
      expect(screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i })).toBeInTheDocument()
    })

    it('should render checkboxes for all tracks', () => {
      renderResultsPage()
      expect(screen.getAllByRole('checkbox')).toHaveLength(3)
    })
  })

  describe('cover song display', () => {
    it('should show cover attribution when a track is a cover', () => {
      setMockLocationState({
        artist: 'The CAB',
        previews: [
          makePreview({
            setlistTrack: '...Baby One More Time',
            plays: 5,
            coverArtist: 'Britney Spears',
          }),
        ],
      })

      renderResultsPage()

      expect(screen.getByText(/Cover · Britney Spears/)).toBeInTheDocument()
    })
  })

  describe('track deselection', () => {
    it('unchecking a track deselects it', async () => {
      renderResultsPage()

      const checkboxes = screen.getAllByRole('checkbox')
      await userEvent.click(checkboxes[0])

      expect(
        screen.getByRole('button', { name: /Create Spotify Playlist \(2 tracks\)/i })
      ).toBeInTheDocument()
    })

    it('disables create button when no tracks are selected', async () => {
      renderResultsPage()

      const checkboxes = screen.getAllByRole('checkbox')
      for (const cb of checkboxes) await userEvent.click(cb)

      expect(screen.getByRole('button', { name: /Create Spotify Playlist \(0 tracks\)/i })).toBeDisabled()
    })
  })

  describe('unmatched tracks', () => {
    it('shows no-match badge for unresolved tracks', () => {
      setMockLocationState({
        artist: 'Band',
        previews: [
          makePreview({
            setlistTrack: 'Unknown Song', plays: 2, matched: false,
            spotifyTrackId: null, spotifyTrackName: null, spotifyArtistName: null,
            confidence: 0, reason: 'NO_RESULTS',
          }),
        ],
      })

      renderResultsPage()

      expect(screen.getByText(/No match found/)).toBeInTheDocument()
    })
  })

  describe('no artist state', () => {
    it('should render no results message when artist is missing', () => {
      setMockLocationState({ artist: null, previews: [] })
      renderResultsPage()
      expect(screen.getByText('No results to display. Please search for an artist.')).toBeInTheDocument()
    })
  })

  describe('no tracks state', () => {
    it('should render no tracks message when tracks array is empty', () => {
      setMockLocationState({ artist: 'Unknown Artist', previews: [] })
      renderResultsPage()
      expect(screen.getByText(/No tracks found for "Unknown Artist"/)).toBeInTheDocument()
    })
  })

  describe('playlist creation', () => {
    it('creates playlist with confirmed track IDs on button click', async () => {
      mockCreatePlaylistFromTracks.mockResolvedValue({
        data: {
          playlistId: 'playlist-123',
          playlistUrl: 'https://open.spotify.com/playlist/playlist-123',
          artist: 'The Beatles',
          tracksAdded: 3,
          createdAt: Date.now().toString(),
        },
      })

      renderResultsPage()

      await userEvent.click(screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i }))

      await waitFor(() =>
        expect(mockCreatePlaylistFromTracks).toHaveBeenCalledWith(
          'The Beatles',
          expect.arrayContaining(['id-Yesterday', 'id-Hey Jude', 'id-Let It Be'])
        )
      )
    })

    it('navigates to playlist page on successful creation', async () => {
      const mockPlaylist = {
        playlistId: 'playlist-123',
        playlistUrl: 'https://open.spotify.com/playlist/playlist-123',
        artist: 'The Beatles',
        tracksAdded: 3,
        createdAt: Date.now().toString(),
      }
      mockCreatePlaylistFromTracks.mockResolvedValue({ data: mockPlaylist })

      renderResultsPage()
      await userEvent.click(screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i }))

      await waitFor(() =>
        expect(mockNavigate).toHaveBeenCalledWith(
          '/playlist',
          { state: expect.objectContaining({ playlist: mockPlaylist }) }
        )
      )
    })

    it('shows error message on creation failure', async () => {
      mockCreatePlaylistFromTracks.mockRejectedValue({
        response: { data: { message: 'Spotify API rate limit exceeded' } },
      })

      renderResultsPage()
      await userEvent.click(screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i }))

      await waitFor(() =>
        expect(screen.getByText('Spotify API rate limit exceeded')).toBeInTheDocument()
      )
    })
  })

  describe('navigation', () => {
    it('should navigate back to search when back button is clicked', async () => {
      renderResultsPage()
      await userEvent.click(screen.getByRole('button', { name: /Back to Search/i }))
      expect(mockNavigate).toHaveBeenCalledWith('/')
    })
  })
})
