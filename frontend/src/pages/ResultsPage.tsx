import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { spotifyApi, TrackPreview } from '../services/api'

// ── Shared option type used by the swap picker ────────────────────────────────

interface SwapOption {
  spotifyTrackId: string
  spotifyTrackName: string
  spotifyArtistName: string | null
  confidence: number
}

// ── Sub-components ────────────────────────────────────────────────────────────

function SpotifyResolution({
  matched,
  trackName,
  artistName,
  isSwapped,
}: {
  matched: boolean
  trackName: string | null
  artistName: string | null
  isSwapped: boolean
}) {
  if (!matched) {
    return (
      <span className="text-xs text-red-500">
        ✗ No match found
      </span>
    )
  }
  return (
    <span className="text-xs flex items-center gap-1 min-w-0">
      <span className={`shrink-0 ${isSwapped ? 'text-blue-400' : 'text-green-500'}`}>→</span>
      <span className="truncate">
        <span className={isSwapped ? 'text-blue-300' : 'text-green-600'}>{trackName}</span>
        {artistName && (
          <span className={isSwapped ? 'text-blue-400' : 'text-green-500'}> · {artistName}</span>
        )}
        {isSwapped && (
          <span className="text-blue-500 ml-1">(swapped)</span>
        )}
      </span>
    </span>
  )
}

function SwapPicker({
  preview,
  currentId,
  onSwap,
}: {
  preview: TrackPreview
  currentId: string | null
  onSwap: (option: SwapOption) => void
}) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // Close dropdown when user clicks outside it
  useEffect(() => {
    if (!open) return
    const handleMouseDown = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleMouseDown)
    return () => document.removeEventListener('mousedown', handleMouseDown)
  }, [open])

  // Build the full option pool: original best match + all alternatives.
  // Exclude whichever one is currently active so the list only shows choices.
  const originalOption: SwapOption | null = preview.matched
    ? {
        spotifyTrackId: preview.spotifyTrackId!,
        spotifyTrackName: preview.spotifyTrackName!,
        spotifyArtistName: preview.spotifyArtistName,
        confidence: preview.confidence,
      }
    : null

  const allOptions: SwapOption[] = [
    ...(originalOption ? [originalOption] : []),
    ...preview.alternatives,
  ]

  const availableOptions = allOptions.filter(o => o.spotifyTrackId !== currentId)

  if (availableOptions.length === 0) return null

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setOpen(o => !o)}
        className="text-xs text-green-500 hover:text-green-700 underline whitespace-nowrap"
        aria-label={`Swap ${preview.setlistTrack}`}
      >
        swap
      </button>

      {open && (
        <div className="absolute right-0 top-6 z-20 bg-gray-800 border border-gray-600 rounded-lg shadow-xl w-72">
          <p className="text-xs text-gray-400 px-3 pt-2 pb-1">Choose a replacement:</p>
          {availableOptions.map(opt => (
            <button
              key={opt.spotifyTrackId}
              onClick={() => { onSwap(opt); setOpen(false) }}
              className="w-full text-left px-3 py-2 hover:bg-gray-700 text-sm"
            >
              <span className="text-white">{opt.spotifyTrackName}</span>
              {opt.spotifyArtistName && (
                <span className="text-gray-400 ml-1">· {opt.spotifyArtistName}</span>
              )}
              <span className="text-gray-500 ml-1 text-xs">({opt.confidence}%)</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const artist = location.state?.artist as string
  const topTracks = Array.isArray(location.state?.topTracks) ? location.state.topTracks : []

  // Preview state
  const [previews, setPreviews] = useState<TrackPreview[] | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState<string | null>(null)

  // Per-track selection & swap overrides
  // Key: setlistTrack name  Value: chosen Spotify track ID (or null if deselected)
  const [selectedIds, setSelectedIds] = useState<Record<string, string | null>>({})

  // Playlist creation state
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  // Load preview as soon as we have an artist
  useEffect(() => {
    if (!artist) return
    setPreviewLoading(true)
    setPreviewError(null)

    spotifyApi.previewPlaylist(artist)
      .then(res => {
        const data = res.data
        setPreviews(data)
        // Default: select every matched track
        const defaults: Record<string, string | null> = {}
        data.forEach(p => {
          defaults[p.setlistTrack] = p.matched ? p.spotifyTrackId : null
        })
        setSelectedIds(defaults)
      })
      .catch(() => setPreviewError('Could not load track preview. Please try again.'))
      .finally(() => setPreviewLoading(false))
  }, [artist])

  // ── Handlers ───────────────────────────────────────────────────────────────

  const toggle = (track: string, defaultId: string | null) => {
    setSelectedIds(prev => {
      const current = prev[track]
      // If currently selected (has an ID), deselect. If null, re-select with default.
      return { ...prev, [track]: current !== null ? null : defaultId }
    })
  }

  const swap = (track: string, option: SwapOption) => {
    setSelectedIds(prev => ({ ...prev, [track]: option.spotifyTrackId }))
  }

  const handleCreatePlaylist = async () => {
    // Sort ascending so the playlist builds toward the most-played closer
    const sortedPreviews = [...(previews ?? [])].sort((a, b) => a.plays - b.plays)
    const selectedPreviews = sortedPreviews.filter(
      p => selectedIds[p.setlistTrack] !== null && selectedIds[p.setlistTrack] !== undefined
    )

    const confirmedIds = selectedPreviews.map(p => selectedIds[p.setlistTrack]!)

    // Build track-level display data so PlaylistPage can show what was added
    const confirmedTracks = selectedPreviews.map(p => {
      const currentId = selectedIds[p.setlistTrack]
      const isSwapped = p.matched && currentId !== null && currentId !== p.spotifyTrackId
      const swapMatch = isSwapped ? p.alternatives.find(a => a.spotifyTrackId === currentId) : null
      return {
        setlistName: p.setlistTrack,
        spotifyName: swapMatch?.spotifyTrackName ?? p.spotifyTrackName,
        spotifyArtist: swapMatch?.spotifyArtistName ?? p.spotifyArtistName,
        isSwapped,
      }
    })

    setCreating(true)
    setCreateError(null)

    try {
      const response = await spotifyApi.createPlaylistFromTracks(artist, confirmedIds)
      navigate('/playlist', { state: { playlist: response.data, confirmedTracks } })
    } catch (err: any) {
      setCreateError(err.response?.data?.message || 'Failed to create playlist')
    } finally {
      setCreating(false)
    }
  }

  // ── Early exits ────────────────────────────────────────────────────────────

  if (!artist) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-4xl mx-auto">
          <button onClick={() => navigate('/')} className="text-blue-600 hover:text-blue-800 mb-4">
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
          <button onClick={() => navigate('/')} className="text-blue-600 hover:text-blue-800 mb-4">
            ← Back to Search
          </button>
          <p className="text-gray-600">
            No tracks found for "{artist}". The artist may not have concert data or may not be available.
          </p>
        </div>
      </div>
    )
  }

  // ── Main render ────────────────────────────────────────────────────────────

  const selectedCount = Object.values(selectedIds).filter(id => id !== null).length

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-4xl mx-auto">
        <button onClick={() => navigate('/')} className="text-blue-600 hover:text-blue-800 mb-6">
          ← Back to Search
        </button>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-3xl font-bold mb-1">{artist}</h1>
          <p className="text-gray-500 text-sm mb-6">
            {topTracks.length} tracks from concert setlists
            {previews && ` · ${selectedCount} selected`}
          </p>

          {/* Preview loading / error banner */}
          {previewLoading && (
            <div className="mb-4 flex items-center gap-2 text-sm text-gray-500">
              <svg className="animate-spin h-4 w-4 text-green-500" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              Resolving tracks against Spotify…
            </div>
          )}

          {previewError && (
            <div className="mb-4 bg-yellow-50 border border-yellow-300 rounded p-3 text-sm text-yellow-800">
              {previewError}
            </div>
          )}

          {createError && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-3 text-red-700 text-sm">
              {createError}
            </div>
          )}

          {/* Track list */}
          <div className="mb-6">
            <h2 className="text-xl font-bold mb-4">Tracks from Concert Setlists</h2>
            <div className="space-y-2">
              {[...(previews ?? topTracks)].sort((a, b) => a.plays - b.plays).map(item => {
                const isPreview = 'setlistTrack' in item
                const trackName = isPreview ? (item as TrackPreview).setlistTrack : (item as { track: string }).track
                const plays = item.plays
                const preview = isPreview ? (item as TrackPreview) : null
                const coverArtist = isPreview ? (item as TrackPreview).coverArtist : null
                const currentId = selectedIds[trackName] ?? null
                const isSelected = currentId !== null
                const defaultId = preview?.matched ? preview.spotifyTrackId : null

                // Determine what's currently active (original match or a swapped-in alternative)
                const isSwapped = preview !== null && preview.matched && currentId !== null && currentId !== preview.spotifyTrackId
                const activeOption: SwapOption | null = (() => {
                  if (!preview) return null
                  if (!isSwapped) return preview.matched ? { spotifyTrackId: preview.spotifyTrackId!, spotifyTrackName: preview.spotifyTrackName!, spotifyArtistName: preview.spotifyArtistName, confidence: preview.confidence } : null
                  return preview.alternatives.find(a => a.spotifyTrackId === currentId) ?? null
                })()

                return (
                  <div
                    key={trackName}
                    data-testid="track-item"
                    className={`p-3 rounded-lg border transition-opacity ${
                      isSelected || !previews
                        ? 'bg-gray-50 border-gray-200'
                        : 'bg-gray-50 border-gray-200 opacity-40'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      {/* Checkbox — only shown once previews are loaded */}
                      {previews && (
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => toggle(trackName, defaultId)}
                          className="mt-1 h-4 w-4 rounded border-gray-300 text-green-600 cursor-pointer"
                          aria-label={`Include ${trackName}`}
                        />
                      )}

                      {/* Track info */}
                      <div className="flex-1 min-w-0">

                        {/* ── setlist.fm row ────────────────────────────── */}
                        <div className="flex justify-between items-start gap-2">
                          <div className="min-w-0">
                            <span className="font-medium text-gray-800 block truncate">{trackName}</span>
                            {coverArtist && (
                              <span className="text-xs text-blue-500">Cover · {coverArtist}</span>
                            )}
                          </div>
                          <span className="text-sm bg-gray-200 text-gray-700 px-2 py-0.5 rounded whitespace-nowrap shrink-0">
                            {plays} plays
                          </span>
                        </div>

                        {/* ── Spotify resolution row ─────────────────────── */}
                        {preview && (
                          <>
                            {/* "── Spotify ──" divider */}
                            <div className="flex items-center gap-2 mt-2">
                              <div className="h-px flex-1 bg-gray-200" />
                              <span className="text-xs font-medium text-green-600 shrink-0">Added to playlist</span>
                              <div className="h-px flex-1 bg-gray-200" />
                            </div>

                            <div className="mt-1 flex items-center justify-between gap-2">
                              <SpotifyResolution
                                matched={preview.matched}
                                trackName={activeOption?.spotifyTrackName ?? preview.spotifyTrackName}
                                artistName={activeOption?.spotifyArtistName ?? preview.spotifyArtistName}
                                isSwapped={isSwapped}
                              />
                              {isSelected && (
                                <SwapPicker
                                  preview={preview}
                                  currentId={currentId}
                                  onSwap={option => swap(trackName, option)}
                                />
                              )}
                            </div>
                          </>
                        )}

                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Create button */}
          <div className="mb-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <p className="text-sm text-gray-600 mb-3">
              {previews
                ? `${selectedCount} of ${previews.length} tracks selected — deselect any you don't want, or swap a bad match using the "swap" link.`
                : 'Preview these tracks before creating your playlist.'}
            </p>
          </div>

          <button
            onClick={handleCreatePlaylist}
            disabled={creating || previewLoading || selectedCount === 0}
            className="w-full bg-green-600 hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-bold py-3 px-4 rounded-lg transition duration-200"
          >
            {creating
              ? 'Creating Playlist…'
              : previewLoading
              ? 'Loading…'
              : `Create Spotify Playlist (${selectedCount} tracks)`}
          </button>
        </div>
      </div>
    </div>
  )
}
