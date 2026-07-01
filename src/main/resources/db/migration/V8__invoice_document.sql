-- Fișierul PDF original încărcat, ca facturile să poată fi descărcate byte-identic.
CREATE TABLE invoice_document (
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT       NOT NULL,
    filename     VARCHAR(255),
    content_type VARCHAR(100),
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

ALTER TABLE invoice ADD COLUMN source_document_id BIGINT;
