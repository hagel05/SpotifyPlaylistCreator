package org.hagelbrand.repository;

import org.hagelbrand.entity.ProviderAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderAccountRepository extends JpaRepository<ProviderAccountEntity, UUID> {

    Optional<ProviderAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<ProviderAccountEntity> findFirstByProviderEmail(String providerEmail);

    List<ProviderAccountEntity> findAllByAppUser_AppUserId(UUID appUserId);

    boolean existsByAppUser_AppUserIdAndProvider(UUID appUserId, String provider);
}
