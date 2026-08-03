CREATE TABLE user_departments (
    user_id    BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    department VARCHAR(255)
);
