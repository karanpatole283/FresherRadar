# Architecture

Single Spring Boot application, package `com.fresherradar`.

- `collector/` sources -> `RawJob`
- `service/CollectionService` ingest + dedupe -> `jobs` (status PENDING)
- `service/ClassificationService` + `classifier/` LLM -> ACTIVE / REJECTED / DUPLICATE / FAILED, cost rows
- `service/JobService` + `JobSpecifications` read side (JPA Specifications on `search_text`)
- `controller/` REST, `exception/` global handler, `scheduler/` daily cron
- Flyway migration `V1__init_schema.sql`: `jobs`, `collection_runs`, `classification_records`

Job lifecycle: PENDING -> (LLM) -> ACTIVE | REJECTED | DUPLICATE; after 3 failed LLM attempts -> FAILED.
