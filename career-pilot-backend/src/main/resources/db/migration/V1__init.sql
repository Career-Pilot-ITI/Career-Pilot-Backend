CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(20),
    verification_code VARCHAR(255),
    verification_code_expires_at TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO roles (name, status) VALUES ('ROLE_USER', TRUE);
