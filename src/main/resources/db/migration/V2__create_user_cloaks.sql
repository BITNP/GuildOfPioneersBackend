CREATE TABLE user_cloaks (
    user_id  BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    cloak_id VARCHAR(255)
);
