-- V4__bank.sql — tranzacții bancare (din extrase)

CREATE TABLE bank_transaction (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    txn_date      DATE          NOT NULL,
    description   VARCHAR(512),
    counterparty  VARCHAR(255),
    direction     VARCHAR(8)    NOT NULL,   -- IN, OUT
    amount        NUMERIC(15,2) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_bank_txn_company ON bank_transaction(company_id);
