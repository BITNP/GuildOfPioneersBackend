ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE TABLE user_oidc_identities (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    issuer VARCHAR(1024) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    CONSTRAINT uk_oidc_identity UNIQUE (issuer, subject)
);
