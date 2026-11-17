# FresherRadar

Fresher job and internship discovery for India. Stage 1 is the frontend; Stage 2 (this repo) adds a Spring Boot backend that collects listings, removes duplicates, classifies them with an LLM, refreshes daily and serves them to the frontend.

```
frontend/   Stage 1 HTML/CSS/vanilla JS (design unchanged)
backend/    Java 21, Spring Boot 3.3, JPA, PostgreSQL, Flyway
docs/       API, architecture, evaluation method
```

## Honest status (read this first)

| Item | Status |
|---|---|
| Backend source code (all layers) | Written |
| Compiled / unit tests executed | **No.** The environment this was generated in had no JDK or Maven Central access. Run `mvn test` yourself first; expect to fix small compile errors. |
| Real jobs collected (target 200+) | **0.** No source credentials were available. Nothing is seeded and no demo data is shipped. |
| Classification accuracy | **Not measured.** Needs a human-labelled set (`docs/EVALUATION.md`). No figure is claimed. |
| Cost per 1,000 jobs | **Not measured.** The app records real token usage per call; the figure appears at `GET /api/admin/metrics/cost` after a real run. |
| `mvnw` / `mvnw.cmd` | Not generated (needs Maven). Run `mvn -N wrapper:wrapper` inside `backend/` once. |

## Run it

1. **PostgreSQL**
   ```sql
   CREATE USER fresherradar WITH PASSWORD 'change-me';
   CREATE DATABASE fresherradar OWNER fresherradar;
   ```
2. **Configure**: copy `.env.example`, set the values, and export them in your shell (or set them in your IDE run config). Minimum for real data:
   - `DB_PASSWORD`
   - `ADZUNA_ENABLED=true`, `ADZUNA_APP_ID`, `ADZUNA_APP_KEY` (free key at developer.adzuna.com)
   - `LLM_API_KEY`, plus `LLM_INPUT_PRICE_PER_M` / `LLM_OUTPUT_PRICE_PER_M` from your provider's price list
   - `ADMIN_TOKEN` (any long random string)
3. **Start the backend**: `cd backend && mvn spring-boot:run` (Flyway creates the tables).
4. **Load data now** (otherwise it runs daily at 02:00 IST):
   ```
   curl -X POST http://localhost:8080/api/admin/refresh -H "X-Admin-Token: <token>"
   ```
5. **Start the frontend**: `cd frontend && python3 -m http.server 5500`, open http://localhost:5500.

Until a refresh has run with both a source and `LLM_API_KEY`, the site shows an empty list ("No data yet"). Collected jobs stay `PENDING` and hidden until the LLM classifies them.

## How it works

1. **Collect**: `AdzunaCollector` (official API) and `RssCollector` (feeds you are permitted to read) return `RawJob`s. Sources are pluggable through the `JobCollector` interface. Scraping is not implemented: add a collector only for sites whose terms/robots.txt allow it.
2. **De-duplicate**: canonical apply-URL hash (unique in the DB) plus a fingerprint of normalised title + company + city, checked in the batch, against the DB, and again after the LLM cleans company/city.
3. **Classify**: `JobClassifier` sends each posting to the LLM with a JSON schema, validates the reply against the frontend vocabulary (category, type, experience), and rejects non-fresher roles or low confidence (`LLM_MIN_CONFIDENCE`). Posting text is treated as untrusted. Every call's tokens and cost go to `classification_records`.
4. **Serve**: only `ACTIVE` jobs are returned. `postedAt` (from the source) and `collectedAt` (when we stored it) are both kept.
5. **Schedule**: `DailyRefreshScheduler` (Spring cron, default `0 0 2 * * *` Asia/Kolkata). One refresh at a time.

## API

| Endpoint | Notes |
|---|---|
| `GET /api/jobs` | Returns `{lastUpdated, jobs, page, size, totalElements, totalPages}`. Params: `search`, `location`, `category`, `employmentType`, `experience`, `company`, `postedWithinHours`, `sort=newest\|oldest`, `page`, `size` (default 50, max 1000) |
| `GET /api/jobs/{id}` | 404 if missing or not active |
| `GET /api/meta` | Filter option values |
| `GET /api/health` | DB status, active/pending counts, last collection, whether the LLM key is set; 503 if the DB is down |
| `POST /api/admin/refresh` | Background refresh (202). Header `X-Admin-Token` |
| `GET /api/admin/runs` | Last 20 collection runs |
| `GET /api/admin/metrics/cost` | Measured tokens, total cost, cost per 1,000 jobs |
| `POST /api/admin/evaluate` | Accuracy against `docs/eval/labelled-jobs.csv` |

Errors use `{timestamp, status, error, message, path}`; stack traces are logged, never returned.

## Frontend changes (minimal)

- `CONFIG.USE_SAMPLE_DATA` set to `false`; `fetchJobs()` now calls `/jobs?size=1000` (the UI still filters in the browser, which suits a few hundred jobs).
- `renderLastUpdated()` handles a null `lastUpdated` (empty database).
- Design, markup and all other behaviour are untouched.

## Attribution

Adzuna's terms require crediting Adzuna where its listings appear. Add a "Jobs by Adzuna" credit to the footer if you use it. Job details belong to the original sources.
