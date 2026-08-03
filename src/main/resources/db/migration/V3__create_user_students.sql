CREATE TABLE user_students (
    user_id    BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    student_id VARCHAR(255)
);
