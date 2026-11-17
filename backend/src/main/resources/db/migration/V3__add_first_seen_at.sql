ALTER TABLE jobs
ADD COLUMN first_seen_at TIMESTAMPTZ;

UPDATE jobs
SET first_seen_at = collected_at
WHERE first_seen_at IS NULL;

ALTER TABLE jobs
ALTER COLUMN first_seen_at SET NOT NULL;

CREATE INDEX idx_jobs_first_seen
ON jobs (first_seen_at DESC);