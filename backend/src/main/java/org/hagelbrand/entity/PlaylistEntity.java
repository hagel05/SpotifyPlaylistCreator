package org.hagelbrand.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists")
public class PlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "playlist_id", updatable = false, nullable = false)
    private UUID playlistId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUserEntity appUser;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_playlist_id", nullable = false)
    private String providerPlaylistId;

    @Column(name = "custom_name", nullable = false)
    private String customName;

    @Column(name = "description")
    private String description;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    /** JSON string — serialised/deserialised by AppUserService via Jackson. */
    @Column(name = "source_data", columnDefinition = "TEXT")
    private String sourceData;

    @ElementCollection
    @CollectionTable(name = "playlist_track_ids", joinColumns = @JoinColumn(name = "playlist_id"))
    @Column(name = "track_id")
    @OrderColumn(name = "position")
    private List<String> trackIds = new ArrayList<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlaylistVersionEntity> versions = new ArrayList<>();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // Getters & setters
    public UUID getPlaylistId() { return playlistId; }
    public AppUserEntity getAppUser() { return appUser; }
    public void setAppUser(AppUserEntity appUser) { this.appUser = appUser; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderPlaylistId() { return providerPlaylistId; }
    public void setProviderPlaylistId(String providerPlaylistId) { this.providerPlaylistId = providerPlaylistId; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceData() { return sourceData; }
    public void setSourceData(String sourceData) { this.sourceData = sourceData; }
    public List<String> getTrackIds() { return trackIds; }
    public void setTrackIds(List<String> trackIds) { this.trackIds = trackIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PlaylistVersionEntity> getVersions() { return versions; }
}
