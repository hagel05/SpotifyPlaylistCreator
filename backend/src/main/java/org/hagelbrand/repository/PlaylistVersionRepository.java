package org.hagelbrand.repository;

import org.hagelbrand.entity.PlaylistVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaylistVersionRepository extends JpaRepository<PlaylistVersionEntity, UUID> {

    List<PlaylistVersionEntity> findAllByPlaylist_PlaylistIdOrderByChangedAtDesc(UUID playlistId);
}
