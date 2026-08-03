CREATE TABLE registration_tickets (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id)
);
