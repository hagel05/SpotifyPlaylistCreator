CREATE TABLE provider_accounts (
    provider_account_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id         UUID        NOT NULL REFERENCES app_users(app_user_id) ON DELETE CASCADE,
    provider            VARCHAR(50) NOT NULL,
    provider_user_id    VARCHAR(255) NOT NULL,
    provider_email      VARCHAR(255),
    linked_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_provider_accounts_app_user ON provider_accounts(app_user_id);
