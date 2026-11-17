ALTER TABLE collection_runs
ADD COLUMN location_rejected INTEGER NOT NULL DEFAULT 0;

ALTER TABLE collection_runs
ADD COLUMN stale_or_invalid INTEGER NOT NULL DEFAULT 0;