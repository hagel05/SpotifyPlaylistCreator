package org.hagelbrand.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_accounts")
public class ProviderAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "provider_account_id", updatable = false, nullable = false)
    private UUID providerAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUserEntity appUser;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "linked_at", updatable = false, nullable = false)
    private Instant linkedAt = Instant.now();

    // Getters & setters
    public UUID getProviderAccountId() { return providerAccountId; }
    public AppUserEntity getAppUser() { return appUser; }
    public void setAppUser(AppUserEntity appUser) { this.appUser = appUser; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }
    public Instant getLinkedAt() { return linkedAt; }
}
