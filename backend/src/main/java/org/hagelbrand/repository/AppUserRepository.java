package org.hagelbrand.repository;

import org.hagelbrand.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    @Query("""
            SELECT u FROM AppUserEntity u
            JOIN u.providerAccounts p
            WHERE p.provider = :provider
              AND p.providerUserId = :providerUserId
            """)
    Optional<AppUserEntity> findByProvider(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );
}
