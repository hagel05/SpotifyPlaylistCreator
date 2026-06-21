CREATE TABLE app_users (
    app_user_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(255),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
