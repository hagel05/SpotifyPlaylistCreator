package org.hagelbrand.repository;

import org.hagelbrand.entity.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<PlaylistEntity, UUID> {

    List<PlaylistEntity> findAllByAppUser_AppUserIdOrderByCreatedAtDesc(UUID appUserId);

    Optional<PlaylistEntity> findByProviderAndProviderPlaylistId(String provider, String providerPlaylistId);
}
