import { useState, useEffect } from 'react'
import { spotifyApi, ArtistSuggestion } from '../services/api'

const MIN_LENGTH = 2

export function useArtistSearch(query: string, debounceMs = 300) {
  const [suggestions, setSuggestions] = useState<ArtistSuggestion[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (query.length < MIN_LENGTH) {
      setSuggestions([])
      return
    }

    const controller = new AbortController()

    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await spotifyApi.searchArtists(query, controller.signal)
        setSuggestions(res.data)
      } catch (err: any) {
        if (!controller.signal.aborted) {
          setSuggestions([])
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }, debounceMs)

    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [query, debounceMs])

  return { suggestions, loading }
}
