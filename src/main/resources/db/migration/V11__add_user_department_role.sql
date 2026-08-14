CREATE TABLE user_departments_new (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    department VARCHAR(255) NOT NULL DEFAULT 'CLINIC',
    role       VARCHAR(255) NOT NULL DEFAULT 'MEMBER',
    CONSTRAINT uk_user_departments_user_department_role UNIQUE (user_id, department, role),
    CONSTRAINT chk_user_departments_department CHECK (department IN ('CLINIC', 'TECH', 'SUPPORT', 'MEDIA', 'PRESIDIUM')),
    CONSTRAINT chk_user_departments_role CHECK (role IN ('LEADER', 'VICE', 'ADVISOR', 'MEMBER'))
);

INSERT INTO user_departments_new (user_id, department, role)
SELECT
    user_id,
    CASE department
        WHEN 'Technology' THEN 'TECH'
        WHEN 'Media' THEN 'MEDIA'
        ELSE 'CLINIC'
    END,
    'MEMBER'
FROM user_departments;

DROP TABLE user_departments;

ALTER TABLE user_departments_new RENAME TO user_departments;
