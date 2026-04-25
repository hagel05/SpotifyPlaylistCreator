import { useLocation, useNavigate } from 'react-router-dom'
import { PlaylistResponse } from '../services/api'

export function PlaylistPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const playlist = location.state?.playlist as PlaylistResponse

  if (!playlist) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 hover:text-blue-800 mb-4"
          >
            ← Back to Search
          </button>
          <p className="text-gray-600">No playlist to display</p>
        </div>
      </div>
    )
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
          <h1 className="text-3xl font-bold mb-2">{playlist.artist} Setlist</h1>
          <p className="text-gray-600 mb-6">{playlist.tracksAdded} tracks added</p>

          <div className="mb-8 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <p className="text-sm text-gray-600 mb-3">Playlist created: {new Date(playlist.createdAt).toLocaleDateString()}</p>
            <a
              href={playlist.playlistUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-block bg-green-600 hover:bg-green-700 text-white font-bold py-3 px-6 rounded-lg transition duration-200"
            >
              Open in Spotify →
            </a>
          </div>

          <div className="mb-6">
            <h2 className="text-xl font-bold mb-4">Tracks in Playlist</h2>
            <div className="space-y-2">
              {playlist.topTracks.map((track, index) => (
                <div key={index} className="bg-gray-50 p-3 rounded-lg border border-gray-200">
                  <div className="flex justify-between items-start">
                    <span className="font-medium text-gray-800">{track.name}</span>
                    <span className="text-sm bg-green-100 text-green-800 px-2 py-1 rounded">
                      {Math.round(track.confidence * 100)}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={() => navigate('/')}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded-lg transition duration-200"
          >
            Create Another Playlist
          </button>
        </div>
      </div>
    </div>
  )
}
