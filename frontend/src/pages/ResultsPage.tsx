import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { spotifyApi, TrackCountResponse } from '../services/api'

export function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const artist = location.state?.artist as string
  const topTracks = Array.isArray(location.state?.topTracks) ? location.state.topTracks : []

  if (!artist) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 hover:text-blue-800 mb-4"
          >
            ← Back to Search
          </button>
          <p className="text-gray-600">No results to display. Please search for an artist.</p>
        </div>
      </div>
    )
  }

  if (topTracks.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 hover:text-blue-800 mb-4"
          >
            ← Back to Search
          </button>
          <p className="text-gray-600">No tracks found for "{artist}". The artist may not have concert data or may not be available.</p>
        </div>
      </div>
    )
  }

  const handleCreatePlaylist = async () => {
    setLoading(true)
    setError(null)

    try {
      const response = await spotifyApi.createPlaylist(artist, 20)
      navigate('/playlist', {
        state: {
          playlist: response.data,
        },
      })
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create playlist')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-4xl mx-auto">
        <button
          onClick={() => navigate('/')}
          className="text-blue-600 hover:text-blue-800 mb-6"
        >
          ← Back to Search
        </button>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-3xl font-bold mb-2">{artist}</h1>
          <p className="text-gray-600 mb-6">Top {topTracks.length} tracks from concert setlists</p>

          <div className="mb-8 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <p className="text-sm text-gray-600 mb-3">Preview these tracks before creating your playlist</p>
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-red-700 text-sm">
                {error}
              </div>
            )}
          </div>

          <div className="mb-6">
            <h2 className="text-xl font-bold mb-4">Tracks from Concert Setlists</h2>
            <div className="space-y-2">
              {topTracks.map((track) => (
                <div key={track.track} className="bg-gray-50 p-3 rounded-lg border border-gray-200">
                  <div className="flex justify-between items-start">
                    <span className="font-medium text-gray-800">{track.track}</span>
                    <span className="text-sm bg-gray-200 text-gray-800 px-2 py-1 rounded">
                      {track.plays} plays
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={handleCreatePlaylist}
            disabled={loading}
            className="w-full bg-green-600 hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-bold py-3 px-4 rounded-lg transition duration-200"
          >
            {loading ? 'Creating Playlist...' : 'Create Spotify Playlist'}
          </button>
        </div>
      </div>
    </div>
  )
}
