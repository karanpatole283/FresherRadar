CREATE TABLE jobs (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(300)  NOT NULL,
    company           VARCHAR(200)  NOT NULL,
    location          VARCHAR(120),
    category          VARCHAR(80),
    employment_type   VARCHAR(30),
    experience        VARCHAR(20),
    posted_at         TIMESTAMPTZ,
    collected_at      TIMESTAMPTZ   NOT NULL,
    source            VARCHAR(80)   NOT NULL,
    source_job_id     VARCHAR(200),
    apply_url         VARCHAR(2000) NOT NULL,
    apply_url_key     VARCHAR(64)   NOT NULL,
    fingerprint       VARCHAR(64)   NOT NULL,
    description       TEXT,
    skills            JSONB         NOT NULL DEFAULT '[]'::jsonb,
    eligibility       JSONB         NOT NULL DEFAULT '[]'::jsonb,
    search_text       TEXT,
    status            VARCHAR(20)   NOT NULL,
    classification_confidence DOUBLE PRECISION,
    classification_reason     VARCHAR(500),
    classified_at     TIMESTAMPTZ,
    classification_attempts   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_jobs_apply_url_key UNIQUE (apply_url_key)
);

CREATE INDEX idx_jobs_status_posted ON jobs (status, posted_at DESC);
CREATE INDEX idx_jobs_fingerprint   ON jobs (fingerprint);
CREATE INDEX idx_jobs_location      ON jobs (location);
CREATE INDEX idx_jobs_category      ON jobs (category);

CREATE TABLE collection_runs (
    id            BIGSERIAL PRIMARY KEY,
    source        VARCHAR(80) NOT NULL,
    started_at    TIMESTAMPTZ NOT NULL,
    finished_at   TIMESTAMPTZ,
    status        VARCHAR(20) NOT NULL,
    fetched       INTEGER NOT NULL DEFAULT 0,
    inserted      INTEGER NOT NULL DEFAULT 0,
    duplicates    INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000)
);

CREATE TABLE classification_records (
    id            BIGSERIAL PRIMARY KEY,
    job_id        BIGINT REFERENCES jobs (id) ON DELETE SET NULL,
    model         VARCHAR(100) NOT NULL,
    input_tokens  INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    cost_usd      NUMERIC(14, 8) NOT NULL DEFAULT 0,
    success       BOOLEAN NOT NULL,
    latency_ms    BIGINT,
    created_at    TIMESTAMPTZ NOT NULL
);
