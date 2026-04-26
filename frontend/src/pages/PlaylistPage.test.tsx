import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import { PlaylistResponse } from '../services/api'

// Use vi.hoisted to properly hoist mock definitions
const { mockNavigate, getMockPlaylistData, setMockPlaylistData } = vi.hoisted(() => {
  let mockPlaylistData: PlaylistResponse | null = {
    playlistId: 'spotify:playlist:123',
    playlistUrl: 'https://open.spotify.com/playlist/123',
    artist: 'The Beatles',
    tracksAdded: 3,
    topTracks: [
      { name: 'Yesterday', confidence: 0.95 },
      { name: 'Hey Jude', confidence: 0.87 },
      { name: 'Let It Be', confidence: 0.92 },
    ],
    createdAt: '2026-04-24T10:30:00Z',
  }

  return {
    mockNavigate: vi.fn(),
    getMockPlaylistData: () => mockPlaylistData,
    setMockPlaylistData: (data: PlaylistResponse | null) => {
      mockPlaylistData = data
    },
  }
})

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({
      state: {
        playlist: getMockPlaylistData(),
      },
    }),
  }
})

import { PlaylistPage } from './PlaylistPage'

describe('PlaylistPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
    setMockPlaylistData({
      playlistId: 'spotify:playlist:123',
      playlistUrl: 'https://open.spotify.com/playlist/123',
      artist: 'The Beatles',
      tracksAdded: 3,
      topTracks: [
        { name: 'Yesterday', confidence: 0.95 },
        { name: 'Hey Jude', confidence: 0.87 },
        { name: 'Let It Be', confidence: 0.92 },
      ],
      createdAt: '2026-04-24T10:30:00Z',
    })
  })

  const renderPlaylistPage = () => {
    return render(
      <BrowserRouter>
        <PlaylistPage />
      </BrowserRouter>
    )
  }

  // Remove the old redundant code that was trying to assign to mockPlaylistData
  // (It was at line 46 and causing errors)

  describe('rendering with valid playlist', () => {
    it('should render artist name with setlist suffix', () => {
      renderPlaylistPage()
      expect(screen.getByText('The Beatles Setlist')).toBeInTheDocument()
    })

    it('should display track count', () => {
      renderPlaylistPage()
      expect(screen.getByText('3 tracks added')).toBeInTheDocument()
    })

    it('should render all tracks with names', () => {
      renderPlaylistPage()

      expect(screen.getByText('Yesterday')).toBeInTheDocument()
      expect(screen.getByText('Hey Jude')).toBeInTheDocument()
      expect(screen.getByText('Let It Be')).toBeInTheDocument()
    })

    it('should display confidence badges as percentages', () => {
      renderPlaylistPage()

      expect(screen.getByText('95%')).toBeInTheDocument()
      expect(screen.getByText('87%')).toBeInTheDocument()
      expect(screen.getByText('92%')).toBeInTheDocument()
    })

    it('should convert timestamp to readable date', () => {
      renderPlaylistPage()

      const expectedDate = new Date('2026-04-24T10:30:00Z').toLocaleDateString()
      expect(screen.getByText(`Playlist created: ${expectedDate}`)).toBeInTheDocument()
    })

    it('should render back to search button', () => {
      renderPlaylistPage()
      const backButtons = screen.getAllByRole('button', { name: /Back to Search/i })
      expect(backButtons.length).toBeGreaterThan(0)
    })

    it('should render open in spotify link', () => {
      renderPlaylistPage()
      const spotifyLink = screen.getByRole('link', { name: /Open in Spotify/i })
      expect(spotifyLink).toHaveAttribute('href', 'https://open.spotify.com/playlist/123')
    })

    it('should open spotify link in new tab', () => {
      renderPlaylistPage()
      const spotifyLink = screen.getByRole('link', { name: /Open in Spotify/i })
      expect(spotifyLink).toHaveAttribute('target', '_blank')
      expect(spotifyLink).toHaveAttribute('rel', 'noopener noreferrer')
    })

    it('should render create another playlist button', () => {
      renderPlaylistPage()
      expect(screen.getByRole('button', { name: /Create Another Playlist/i })).toBeInTheDocument()
    })

    it('should display section heading for tracks', () => {
      renderPlaylistPage()
      expect(screen.getByText('Tracks in Playlist')).toBeInTheDocument()
    })
  })

  describe('playlist with no tracks', () => {
    it('should handle empty tracks array', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        tracksAdded: 0,
        topTracks: [],
      })

      renderPlaylistPage()

      expect(screen.getByText('0 tracks added')).toBeInTheDocument()
      expect(screen.getByText('Tracks in Playlist')).toBeInTheDocument()
    })
  })

  describe('no playlist state', () => {
    it('should show no playlist message when data is missing', () => {
      setMockPlaylistData(null as any)

      render(
        <BrowserRouter>
          <PlaylistPage />
        </BrowserRouter>
      )

      expect(screen.getByText('No playlist to display')).toBeInTheDocument()
    })

    it('should show back to search button on no playlist state', () => {
      setMockPlaylistData(null as any)

      render(
        <BrowserRouter>
          <PlaylistPage />
        </BrowserRouter>
      )

      expect(screen.getByRole('button', { name: /Back to Search/i })).toBeInTheDocument()
    })
  })

  describe('navigation', () => {
    it('should navigate to home when back button is clicked', async () => {
      renderPlaylistPage()
      const backButtons = screen.getAllByRole('button', { name: /Back to Search/i })

      await userEvent.click(backButtons[0])

      expect(mockNavigate).toHaveBeenCalledWith('/')
    })

    it('should navigate to home when create another playlist button is clicked', async () => {
      renderPlaylistPage()
      const createAnotherButton = screen.getByRole('button', { name: /Create Another Playlist/i })

      await userEvent.click(createAnotherButton)

      expect(mockNavigate).toHaveBeenCalledWith('/')
    })
  })

  describe('confidence score formatting', () => {
    it('should round confidence to nearest whole percentage', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        topTracks: [
          { name: 'Track 1', confidence: 0.856 },
          { name: 'Track 2', confidence: 0.844 },
        ],
      })

      renderPlaylistPage()

      expect(screen.getByText('86%')).toBeInTheDocument()
      expect(screen.getByText('84%')).toBeInTheDocument()
    })

    it('should handle confidence value of 1.0', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        topTracks: [{ name: 'Perfect Track', confidence: 1.0 }],
      })

      renderPlaylistPage()

      expect(screen.getByText('100%')).toBeInTheDocument()
    })

    it('should handle confidence value of 0.0', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        topTracks: [{ name: 'No Confidence', confidence: 0.0 }],
      })

      renderPlaylistPage()

      expect(screen.getByText('0%')).toBeInTheDocument()
    })
  })

  describe('playlist metadata', () => {
    it('should display correct playlist ID in data', () => {
      renderPlaylistPage()
      // PlaylistId is not directly visible in UI, but spotify link uses it
      const spotifyLink = screen.getByRole('link', { name: /Open in Spotify/i })
      expect(spotifyLink.getAttribute('href')).toContain('123')
    })

    it('should handle long artist names', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        artist: 'Alicia Keys and The Roots featuring Common and Questlove',
      })

      renderPlaylistPage()

      expect(screen.getByText('Alicia Keys and The Roots featuring Common and Questlove Setlist')).toBeInTheDocument()
    })

    it('should handle many tracks gracefully', () => {
      const current = getMockPlaylistData()
      setMockPlaylistData({
        ...(current as PlaylistResponse),
        tracksAdded: 100,
        topTracks: Array.from({ length: 100 }, (_, i) => ({
          name: `Track ${i + 1}`,
          confidence: 0.8,
        })),
      })

      renderPlaylistPage()

      expect(screen.getByText('100 tracks added')).toBeInTheDocument()
      expect(screen.getByText('Track 1')).toBeInTheDocument()
      expect(screen.getByText('Track 100')).toBeInTheDocument()
    })
  })
})
