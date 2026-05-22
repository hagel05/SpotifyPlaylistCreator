import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'

const {
  mockPreviewPlaylist,
  mockCreatePlaylistFromTracks,
  mockNavigate,
  getMockLocationState,
  setMockLocationState,
} = vi.hoisted(() => {
  let mockLocationState = {
    artist: 'The Beatles',
    topTracks: [
      { track: 'Yesterday', plays: 150 },
      { track: 'Hey Jude', plays: 120 },
      { track: 'Let It Be', plays: 100 },
    ],
  }

  return {
    mockPreviewPlaylist: vi.fn(),
    mockCreatePlaylistFromTracks: vi.fn(),
    mockNavigate: vi.fn(),
    getMockLocationState: () => mockLocationState,
    setMockLocationState: (state: any) => { mockLocationState = state },
  }
})

vi.mock('../services/api', () => ({
  spotifyApi: {
    previewPlaylist: mockPreviewPlaylist,
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
    mockPreviewPlaylist.mockClear()
    mockCreatePlaylistFromTracks.mockClear()
    mockNavigate.mockClear()
    setMockLocationState({
      artist: 'The Beatles',
      topTracks: [
        { track: 'Yesterday', plays: 150 },
        { track: 'Hey Jude', plays: 120 },
        { track: 'Let It Be', plays: 100 },
      ],
    })
    mockPreviewPlaylist.mockResolvedValue({ data: DEFAULT_PREVIEWS })
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

    it('should render all tracks with their play counts', async () => {
      renderResultsPage()

      // Wait for preview to load (track name appears in both the label and the resolution badge)
      await waitFor(() => expect(screen.getAllByText('Yesterday').length).toBeGreaterThan(0))

      expect(screen.getAllByText('Yesterday')[0]).toBeInTheDocument()
      expect(screen.getByText('150 plays')).toBeInTheDocument()
      expect(screen.getAllByText('Hey Jude')[0]).toBeInTheDocument()
      expect(screen.getByText('120 plays')).toBeInTheDocument()
      expect(screen.getAllByText('Let It Be')[0]).toBeInTheDocument()
      expect(screen.getByText('100 plays')).toBeInTheDocument()
    })

    it('should display tracks in ascending play-count order (lowest first, highest last)', async () => {
      // Previews arrive in the order the backend returns them (highest-first from setlist.fm)
      mockPreviewPlaylist.mockResolvedValue({
        data: [
          makePreview({ setlistTrack: 'Yesterday', plays: 150 }),
          makePreview({ setlistTrack: 'Hey Jude', plays: 120 }),
          makePreview({ setlistTrack: 'Let It Be', plays: 100 }),
        ],
      })

      renderResultsPage()

      // Wait for previews to load and be sorted
      await waitFor(() => expect(screen.getAllByTestId('track-item')).toHaveLength(3))

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

    it('should render create playlist button', async () => {
      renderResultsPage()
      // Button label includes track count once preview resolves
      await waitFor(() =>
        expect(screen.getByRole('button', { name: /Create Spotify Playlist/i })).toBeInTheDocument()
      )
    })
  })

  describe('cover song display', () => {
    it('should show cover attribution when a track is a cover', async () => {
      mockPreviewPlaylist.mockResolvedValue({
        data: [
          makePreview({
            setlistTrack: '...Baby One More Time',
            plays: 5,
            coverArtist: 'Britney Spears',
          }),
        ],
      })

      setMockLocationState({
        artist: 'The CAB',
        topTracks: [{ track: '...Baby One More Time', plays: 5 }],
      })

      renderResultsPage()

      await waitFor(() =>
        expect(screen.getByText(/Cover · Britney Spears/)).toBeInTheDocument()
      )
    })
  })

  describe('track deselection', () => {
    it('shows checkboxes after previews load', async () => {
      renderResultsPage()

      await waitFor(() =>
        expect(screen.getAllByRole('checkbox')).toHaveLength(3)
      )
    })

    it('unchecking a track deselects it', async () => {
      renderResultsPage()

      const checkboxes = await waitFor(() => screen.getAllByRole('checkbox'))
      await userEvent.click(checkboxes[0])

      // Selected count in button label should drop
      expect(
        screen.getByRole('button', { name: /Create Spotify Playlist \(2 tracks\)/i })
      ).toBeInTheDocument()
    })

    it('disables create button when no tracks are selected', async () => {
      renderResultsPage()

      const checkboxes = await waitFor(() => screen.getAllByRole('checkbox'))
      for (const cb of checkboxes) await userEvent.click(cb)

      expect(screen.getByRole('button', { name: /Create Spotify Playlist \(0 tracks\)/i })).toBeDisabled()
    })
  })

  describe('unmatched tracks', () => {
    it('shows no-match badge for unresolved tracks', async () => {
      mockPreviewPlaylist.mockResolvedValue({
        data: [
          makePreview({ setlistTrack: 'Unknown Song', plays: 2, matched: false,
            spotifyTrackId: null, spotifyTrackName: null, spotifyArtistName: null,
            confidence: 0, reason: 'NO_RESULTS' }),
        ],
      })

      setMockLocationState({
        artist: 'Band',
        topTracks: [{ track: 'Unknown Song', plays: 2 }],
      })

      renderResultsPage()

      await waitFor(() =>
        expect(screen.getByText(/No match found/)).toBeInTheDocument()
      )
    })
  })

  describe('no artist state', () => {
    it('should render no results message when artist is missing', () => {
      setMockLocationState({ artist: null, topTracks: [] })
      renderResultsPage()
      expect(screen.getByText('No results to display. Please search for an artist.')).toBeInTheDocument()
    })
  })

  describe('no tracks state', () => {
    it('should render no tracks message when tracks array is empty', () => {
      setMockLocationState({ artist: 'Unknown Artist', topTracks: [] })
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

      await waitFor(() => screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i }))
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

      await waitFor(() => screen.getByRole('button', { name: /Create Spotify Playlist/i }))
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

      await waitFor(() => screen.getByRole('button', { name: /Create Spotify Playlist \(3 tracks\)/i }))
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
