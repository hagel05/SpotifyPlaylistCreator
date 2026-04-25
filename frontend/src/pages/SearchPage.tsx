import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { spotifyApi } from '../services/api'

export function SearchPage() {
  const [artist, setArtist] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const navigate = useNavigate()

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
    // Redirect to backend's OAuth endpoint (use 127.0.0.1 to match Spotify app config)
    window.location.href = 'http://127.0.0.1:8080/oauth2/authorization/spotify'
  }

  const handleLogout = () => {
    // Logout endpoint
    window.location.href = 'http://127.0.0.1:8080/logout'
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
            <button
              onClick={handleSpotifyLogin}
              className="mt-4 bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded-lg transition duration-200"
            >
              🎵 Login with Spotify
            </button>
          )}
        </div>

        <form onSubmit={handleSearch} className="space-y-4">
          <div>
            <input
              type="text"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              placeholder="Search for an artist..."
              className="w-full px-4 py-3 rounded-lg bg-green-900 text-white placeholder-green-300 border border-green-700 focus:outline-none focus:border-green-500 focus:ring-2 focus:ring-green-500"
            />
          </div>

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
      </div>
    </div>
  )
}
