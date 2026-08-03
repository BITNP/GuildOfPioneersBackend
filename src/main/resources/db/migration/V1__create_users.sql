CREATE TABLE users (
    id        BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    avatar    VARCHAR(255) NOT NULL,
    phone     VARCHAR(255) NOT NULL,
    email     VARCHAR(255),
    password  VARCHAR(255) NOT NULL
);
