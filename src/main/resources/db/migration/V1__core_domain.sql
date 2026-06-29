-- V1__core_domain.sql

CREATE TABLE company (
    id            BIGSERIAL PRIMARY KEY,
    cui           VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    company_type  VARCHAR(32)  NOT NULL,   -- SRL, PFA, II
    tax_regime    VARCHAR(32)  NOT NULL,   -- MICRO_1, MICRO_3, PROFIT_16, PFA
    vat_payer     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE invoice (
    id             BIGSERIAL PRIMARY KEY,
    company_id     BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    direction      VARCHAR(16)   NOT NULL,  -- ISSUED, RECEIVED
    invoice_number VARCHAR(64),
    partner_name   VARCHAR(255),
    partner_cui    VARCHAR(32),
    issue_date     DATE          NOT NULL,
    due_date       DATE,
    net_amount     NUMERIC(15,2) NOT NULL,
    vat_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    gross_amount   NUMERIC(15,2) NOT NULL,
    category       VARCHAR(64),
    deductible     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_company ON invoice(company_id);

CREATE TABLE employee (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    full_name     VARCHAR(255)  NOT NULL,
    cnp           VARCHAR(13),
    gross_salary  NUMERIC(15,2) NOT NULL,
    position      VARCHAR(128),
    start_date    DATE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_employee_company ON employee(company_id);

CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    description  VARCHAR(255),
    category     VARCHAR(64),
    amount       NUMERIC(15,2) NOT NULL,
    expense_date DATE          NOT NULL,
    deductible   BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_expense_company ON expense(company_id);
