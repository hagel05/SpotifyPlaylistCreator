package org.hagelbrand.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlist_versions")
public class PlaylistVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id", updatable = false, nullable = false)
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @ElementCollection
    @CollectionTable(name = "playlist_version_track_ids", joinColumns = @JoinColumn(name = "version_id"))
    @Column(name = "track_id")
    @OrderColumn(name = "position")
    private List<String> trackIds = new ArrayList<>();

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "changed_at", updatable = false, nullable = false)
    private Instant changedAt = Instant.now();

    // Getters & setters
    public UUID getVersionId() { return versionId; }
    public PlaylistEntity getPlaylist() { return playlist; }
    public void setPlaylist(PlaylistEntity playlist) { this.playlist = playlist; }
    public List<String> getTrackIds() { return trackIds; }
    public void setTrackIds(List<String> trackIds) { this.trackIds = trackIds; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Instant getChangedAt() { return changedAt; }
}
