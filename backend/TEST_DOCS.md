# Test Examples & Usage Guide

## Running the New Integration Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
# Setlist.fm Service Integration Tests
./gradlew test --tests SetlistFmServiceImplIntegrationTest

# Track Aggregation Tests
./gradlew test --tests SetlistTrackServiceIntegrationTest

# Spotify Track Resolver Tests
./gradlew test --tests SpotifyTrackResolverIntegrationTest

# Playlist Orchestrator Tests
./gradlew test --tests SpotifyPlaylistOrchestratorIntegrationTest

# Functional/E2E Tests
./gradlew test --tests PlaylistControllerFunctionalTest
```

### Run Specific Test Method
```bash
./gradlew test --tests SpotifyTrackResolverIntegrationTest.matchesTrackWithExactName
```

### Run All Tests with Verbose Output
```bash
./gradlew test --info
```

### Generate HTML Test Report
```bash
./gradlew test
# Report will be at: build/reports/tests/test/index.html
```

---

## Test Examples & What They Validate

### 1. SetlistFm API Integration

**Test**: `searchesArtistsAndDeserializesResponse()`
```
Simulates: GET /search/artists?artistName=Green Day
Response: JSON with artist MBID, name, URL
Validates: Correct JSON deserialization into ArtistSearchResponse
```

**Test**: `retrievesSetlistsForArtist()`
```
Simulates: GET /artist/{mbid}/setlists
Response: Complex nested JSON with venues, cities, songs
Validates: Proper handling of State deserialization (string vs object)
Validates: Song list extraction from multiple sets
```

### 2. Track Aggregation

**Test**: `aggregatesMostPlayedTracksFromMultipleSetlists()`
```
Input: 3 setlists with overlapping songs
Processing: Song.A appears 3x, Song.B appears 2x, etc.
Validates: Correct counting and grouping
Expected Output: Map<String, Long> with correct frequencies
```

**Test**: `ranksTopTracksCorrectly()`
```
Input: Unordered track counts
Processing: Sort by play count (descending) and limit
Validates: Top 3 tracks are: Song.C(8), Song.A(5), Song.B(3)
```

### 3. Spotify Track Matching

**Test**: `matchesTrackWithExactName()`
```
Input: "When I Come Around" by "Green Day"
Spotify Response: Exact match found
Validates: confidence > 50, reason contains "EXACT_TRACK"
Validates: spotifyTrackId is populated
```

**Test**: `rejectTrackWithLowConfidence()`
```
Input: Searching for a track with weak matches
Spotify Response: Only poor matches available
Validates: matched = false, reason = "LOW_CONFIDENCE"
Validates: Returns null for spotifyTrackId
```

**Test**: `normalizesTrackNamesBeforeMatching()`
```
Input: "Clandestin&" (with accents and punctuation)
Processing: Normalize to "clandestino"
Spotify Response: "Clandestino" (without accents)
Validates: Normalization enables the match
```

### 4. Playlist Orchestration

**Test**: `buildPlaylistResolvesTracksAndCreatesPlaylist()`
```
Input: 
  - Artist: "Green Day"
  - Tracks: [When I Come Around, Basket Case, American Idiot]
  
Processing:
  1. Get user ID from OAuth token
  2. Resolve each track through Spotify
  3. Create playlist with name "Green Day – Predicted Setlist"
  4. Add matched track IDs to playlist

Validates:
  - All 3 tracks resolved
  - Playlist created with correct name
  - Only matched tracks added (excludes unmatched)
```

**Test**: `excludesUnmatchedTracksFromPlaylist()`
```
Input:
  - Tracks: [matched_track, unmatched_track, matched_track]
  
Processing: Filters out unmatched tracks before adding
Validates: Only 2 track IDs added to playlist (unmatched excluded)
```

### 5. End-to-End Controller Flow

**Test**: `createsPlaylistSuccessfullyWithMultipleTracks()`
```
HTTP Request: 
  GET /api/playlist/Green%20Day/spotify-playlist?limit=5
  Header: Authorization (OAuth2)

Processing:
  1. Controller validates artist & limit
  2. Service gets top 5 tracks from setlist.fm
  3. Orchestrator creates Spotify playlist
  4. Returns playlist ID and URL

Response:
  HTTP 200 OK
  {
    "playlistId": "playlist-greenday-123",
    "url": "https://open.spotify.com/playlist/playlist-greenday-123"
  }
```

**Test**: `sortTracksBeforeCreatingPlaylist()`
```
Input Tracks: [Everlong(5), The Pretender(8), Best of You(6)]
Expected Sort: [The Pretender(8), Best of You(6), Everlong(5)]
Validates: Tracks passed to orchestrator in correct order
```

---

## Understanding Test Assertions

### AssertJ Examples Used

```java
// Checking response is not null and values match
assertThat(response).isNotNull();
assertThat(response.total()).isEqualTo(1);
assertThat(response.artists()).hasSize(1);

// Complex assertions
assertThat(topTracks).hasSize(3);
assertThat(topTracks.get(0).plays()).isEqualTo(8L);

// Verification of mocked service calls
verify(playlistService).createPlaylist(spotifyClient, "user-123", artist);
verify(playlistService).addTracks(
    spotifyClient,
    "playlist-456",
    List.of("spotify-id-1", "spotify-id-2")
);

// Filtering and extraction
assertThat(filteredSetlists).hasSizeGreaterThanOrEqualTo(1);
assertThat(songs).extracting(Song::name)
    .contains("When I Come Around", "Basket Case");
```

---

## Debugging Failed Tests

If a test fails:

1. **Check the assertion message** - Shows expected vs actual
   ```
   Expected: playlist ID "123"
   Actual: "456"
   ```

2. **Review mock setup** - Verify mocked responses match expectations
   ```java
   when(spotifyService.searchTrack("Artist", "Song"))
       .thenReturn(response);  // Check this returns what test expects
   ```

3. **Check JSON formatting** - API integration tests use MockWebServer
   ```
   Ensure mock JSON is properly formatted
   Check all required fields are present
   ```

4. **Verify dependencies** - Ensure test dependencies are in build.gradle.kts
   ```
   MockWebServer, Mockito, AssertJ should be testImplementation
   ```

---

## Test Isolation

- Each test uses fresh mocks (via @BeforeEach)
- MockWebServer is reset after each test (@AfterEach)
- No test state carries over to next test
- Tests can run in any order independently

---

## Coverage Metrics

To see test coverage:

```bash
./gradlew test jacocoTestReport
# Report at: build/reports/jacoco/test/html/index.html
```

This shows:
- % of code paths exercised
- Line coverage per class
- Branch coverage

---

**Questions?** Refer to individual test classes for detailed Javadoc comments explaining each test case.

