-- Fereastră glisantă de consum: FREE se resetează la câteva ore, planurile plătite lunar.
ALTER TABLE usage_quota ADD COLUMN window_start TIMESTAMP NOT NULL DEFAULT now();
