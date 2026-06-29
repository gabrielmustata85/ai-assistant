CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

ALTER TABLE company ADD COLUMN owner_user_id BIGINT REFERENCES app_user(id);
CREATE INDEX idx_company_owner ON company(owner_user_id);
