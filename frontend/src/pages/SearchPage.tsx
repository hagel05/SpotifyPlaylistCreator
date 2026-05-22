import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Combobox, ComboboxInput, ComboboxOption, ComboboxOptions } from '@headlessui/react'
import { spotifyApi } from '../services/api'
import { useArtistSearch } from '../hooks/useArtistSearch'

export function SearchPage() {
  const [artist, setArtist] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const navigate = useNavigate()

  const { suggestions } = useArtistSearch(artist)

  useEffect(() => {
    checkAuthentication()
  }, [])

  const checkAuthentication = async () => {
    try {
      const response = await spotifyApi.checkAuth()
      setIsAuthenticated(response.data?.authenticated || false)
    } catch (err) {
      setIsAuthenticated(false)
    }
  }

  const handleSpotifyLogin = () => {
    window.location.href = '/oauth2/authorization/spotify'
  }

  const handleLogout = () => {
    window.location.href = '/logout'
  }

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!artist.trim()) return

    setLoading(true)
    setError(null)

    try {
      const response = await spotifyApi.getTopTracks(artist)
      const tracks = response.data.trackCounts || []
      if (!tracks || tracks.length === 0) {
        setError('No concert data found for this artist')
        return
      }
      navigate('/results', {
        state: {
          artist,
          topTracks: tracks,
        },
      })
    } catch (err: any) {
      console.error('Search error:', err)
      const errorMsg = err.response?.data?.message || err.message || 'Failed to fetch tracks'
      setError(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-900 to-green-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">Concert Playlist Builder</h1>
          <p className="text-green-200">Create playlists from concert setlists</p>

          {isAuthenticated ? (
            <div className="mt-4 space-y-2">
              <p className="text-green-300">✓ Logged in with Spotify</p>
              <button
                onClick={handleLogout}
                className="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded-lg transition duration-200"
              >
                Logout
              </button>
            </div>
          ) : (
            <div className="mt-6 space-y-3">
              <p className="text-green-200 text-sm">Connect your Spotify account to search for artists and create playlists</p>
              <button
                onClick={handleSpotifyLogin}
                className="mt-2 bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-6 rounded-lg transition duration-200"
              >
                🎵 Login with Spotify
              </button>
            </div>
          )}
        </div>

        {isAuthenticated && (
          <form onSubmit={handleSearch} className="space-y-4">
            <Combobox immediate value={artist} onChange={(value) => { if (value !== null) setArtist(value) }}>
              <div className="relative">
                <ComboboxInput
                  className="w-full px-4 py-3 rounded-lg bg-green-900 text-white placeholder-green-300 border border-green-700 focus:outline-none focus:border-green-500 focus:ring-2 focus:ring-green-500"
                  placeholder="Search for an artist..."
                  displayValue={(v: string) => v}
                  onChange={(e) => setArtist(e.target.value)}
                />
                {suggestions.length > 0 && (
                  <ComboboxOptions
                    portal={false}
                    className="absolute z-10 w-full mt-1 bg-green-900 border border-green-700 rounded-lg overflow-hidden shadow-lg"
                  >
                    {suggestions.map((a) => (
                      <ComboboxOption
                        key={a.id}
                        value={a.name}
                        className={({ focus }) =>
                          `flex items-center gap-3 px-4 py-2 cursor-pointer ${focus ? 'bg-green-700' : ''}`
                        }
                      >
                        {a.imageUrl ? (
                          <img
                            src={a.imageUrl}
                            alt={a.name}
                            className="w-8 h-8 rounded-full object-cover flex-shrink-0"
                          />
                        ) : (
                          <div className="w-8 h-8 rounded-full bg-green-800 flex-shrink-0" />
                        )}
                        <span className="text-white text-sm">{a.name}</span>
                      </ComboboxOption>
                    ))}
                  </ComboboxOptions>
                )}
              </div>
            </Combobox>

            {error && (
              <div className="bg-red-900/50 border border-red-600 rounded-lg p-3 text-red-100 text-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !artist.trim()}
              className="w-full bg-green-600 hover:bg-green-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold py-3 px-4 rounded-lg transition duration-200"
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
