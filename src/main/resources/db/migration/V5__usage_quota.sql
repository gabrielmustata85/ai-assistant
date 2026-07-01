-- Consum de tokens per utilizator, resetat lunar (perioada = YYYY-MM).
CREATE TABLE usage_quota (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL UNIQUE,
    period_ym    VARCHAR(7)  NOT NULL,
    tokens_used  BIGINT      NOT NULL DEFAULT 0,
    token_limit  BIGINT      NOT NULL
);
