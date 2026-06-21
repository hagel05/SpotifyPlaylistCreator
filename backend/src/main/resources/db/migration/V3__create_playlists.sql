CREATE TABLE playlists (
    playlist_id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id          UUID        NOT NULL REFERENCES app_users(app_user_id) ON DELETE CASCADE,
    provider             VARCHAR(50) NOT NULL,
    provider_playlist_id VARCHAR(255) NOT NULL,
    custom_name          VARCHAR(255) NOT NULL,
    description          TEXT,
    source_type          VARCHAR(50),
    source_data          TEXT,
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_playlist UNIQUE (provider, provider_playlist_id)
);

CREATE INDEX idx_playlists_app_user ON playlists(app_user_id);

-- Track IDs ordered by position
CREATE TABLE playlist_track_ids (
    playlist_id UUID    NOT NULL REFERENCES playlists(playlist_id) ON DELETE CASCADE,
    track_id    VARCHAR(255) NOT NULL,
    position    INTEGER NOT NULL,
    PRIMARY KEY (playlist_id, position)
);

CREATE TABLE playlist_versions (
    version_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID        NOT NULL REFERENCES playlists(playlist_id) ON DELETE CASCADE,
    change_type VARCHAR(50) NOT NULL,
    changed_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_playlist_versions_playlist ON playlist_versions(playlist_id);

-- Track IDs for each version snapshot
CREATE TABLE playlist_version_track_ids (
    version_id UUID    NOT NULL REFERENCES playlist_versions(version_id) ON DELETE CASCADE,
    track_id   VARCHAR(255) NOT NULL,
    position   INTEGER NOT NULL,
    PRIMARY KEY (version_id, position)
);
