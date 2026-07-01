-- Colaboratori (parteneri) ai firmei, auto-populați din facturile încărcate.
CREATE TABLE partner (
    id         BIGSERIAL PRIMARY KEY,
    company_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    cui        VARCHAR(32),
    iban       VARCHAR(34),
    phone      VARCHAR(32),
    email      VARCHAR(128),
    address    VARCHAR(512),
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_partner_company ON partner (company_id);
