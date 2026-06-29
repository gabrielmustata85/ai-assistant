-- V2__knowledge_and_interactions.sql

CREATE TABLE knowledge_document (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    source      VARCHAR(255),
    namespace   VARCHAR(64)  NOT NULL DEFAULT 'legislation',
    uploaded_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE ai_response_history (
    id                   BIGSERIAL PRIMARY KEY,
    session_id           VARCHAR(64) NOT NULL,
    company_id           BIGINT,
    user_query           TEXT        NOT NULL,
    ai_response          TEXT,
    data_gaps            TEXT,
    timestamp            TIMESTAMP   NOT NULL DEFAULT now(),
    was_corrected        BOOLEAN     DEFAULT FALSE,
    corrected_response   TEXT,
    correction_timestamp TIMESTAMP,
    embedding_vector     TEXT,
    metadata             JSON
);
CREATE INDEX idx_history_session ON ai_response_history(session_id);
