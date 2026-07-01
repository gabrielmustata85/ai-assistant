-- Planul curent al userului (FREE / PRO / MAX). Determină limita de tokens.
ALTER TABLE usage_quota ADD COLUMN plan VARCHAR(16) NOT NULL DEFAULT 'FREE';
