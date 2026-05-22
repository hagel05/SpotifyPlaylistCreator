import { renderHook, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useArtistSearch } from './useArtistSearch'

const { mockSearchArtists } = vi.hoisted(() => ({
  mockSearchArtists: vi.fn(),
}))

vi.mock('../services/api', () => ({
  spotifyApi: {
    searchArtists: mockSearchArtists,
  },
}))

describe('useArtistSearch', () => {
  beforeEach(() => {
    mockSearchArtists.mockClear()
  })

  it('returns empty suggestions for empty query', () => {
    const { result } = renderHook(() => useArtistSearch('', 0))
    expect(result.current.suggestions).toEqual([])
    expect(mockSearchArtists).not.toHaveBeenCalled()
  })

  it('returns empty suggestions for query shorter than 2 characters', () => {
    const { result } = renderHook(() => useArtistSearch('a', 0))
    expect(result.current.suggestions).toEqual([])
    expect(mockSearchArtists).not.toHaveBeenCalled()
  })

  it('does not fetch before the debounce delay elapses', () => {
    vi.useFakeTimers()
    mockSearchArtists.mockResolvedValue({ data: [] })

    renderHook(() => useArtistSearch('Beatles')) // default 300ms

    vi.advanceTimersByTime(299)
    expect(mockSearchArtists).not.toHaveBeenCalled()

    vi.useRealTimers()
  })

  it('fetches suggestions after the debounce delay', async () => {
    const mockSuggestions = [
      { id: '1', name: 'The Beatles', imageUrl: 'https://example.com/beatles.jpg' },
    ]
    mockSearchArtists.mockResolvedValue({ data: mockSuggestions })

    const { result } = renderHook(() => useArtistSearch('Beatles', 0))

    await waitFor(() => expect(result.current.suggestions).toEqual(mockSuggestions))
    expect(mockSearchArtists).toHaveBeenCalledWith('Beatles', expect.any(AbortSignal))
  })

  it('returns empty suggestions on API error', async () => {
    mockSearchArtists.mockRejectedValue(new Error('Network error'))

    const { result } = renderHook(() => useArtistSearch('Beatles', 0))

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.suggestions).toEqual([])
  })

  it('resets suggestions when query drops below minimum length', async () => {
    const mockSuggestions = [{ id: '1', name: 'The Beatles', imageUrl: null }]
    mockSearchArtists.mockResolvedValue({ data: mockSuggestions })

    const { result, rerender } = renderHook(({ q }) => useArtistSearch(q, 0), {
      initialProps: { q: 'Beatles' },
    })

    await waitFor(() => expect(result.current.suggestions).toEqual(mockSuggestions))

    act(() => rerender({ q: 'B' }))
    expect(result.current.suggestions).toEqual([])
  })
})
