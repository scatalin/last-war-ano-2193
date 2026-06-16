# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build fat JAR
mvn package

# Run (dev)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=LastWarApplicationTests

# Build and run the JAR directly
mvn package -DskipTests && java -jar target/ano2193-*.jar
```

The app starts on `http://localhost:8080`. H2 console is at `/h2-console` (JDBC URL: `jdbc:h2:file:./data/appdb`).

## Architecture

Standard Spring Boot 3.2.5 / Java 17 layered app. No Lombok — all getters/setters are explicit.

**Request flow:** Browser → `SecurityFilterChain` → `@Controller` → `@Service` → Spring Data JPA repository → H2 file-mode DB (`./data/appdb.mv.db`)

**Three roles (enforced in `SecurityConfig`):**
- `VIEWER` — read-only: `/dashboard`, `/rankings`
- `SUBMITTER` — adds `VIEWER` + access to `/upload`
- `ADMIN` — full access including `/admin/**`

Default users are seeded by `DataInitializer` on first startup: `admin/admin123`, `submitter/sub123`, `viewer/view123`.

**CSV dual-persistence (`CsvService`):** On startup, if the DB is empty it imports `./data/rankings.csv`. On shutdown (`@PreDestroy`) and every 5 minutes (`CsvPersistenceScheduler`) it exports the DB back to that file. This means the CSV is the durable backup across restarts when using the H2 file DB.

**OCR stub (`ImageParsingService`):** `parseImage()` currently returns an empty list — the Tesseract integration point is clearly marked with comments. `parseOcrText()` contains the heuristic parser that will be wired in once OCR is enabled (add `tess4j` dependency + install `tesseract-ocr` on the host).

**File storage:** Uploaded images are renamed to a UUID and stored under `./uploads/`. The original filename and processing status (`PROCESSING` → `PROCESSED` / `FAILED`) are tracked in the `PhotoUpload` entity.

**Key directories at runtime (relative to working directory):**
- `./data/appdb.mv.db` — H2 database
- `./data/rankings.csv` — CSV snapshot
- `./uploads/` — uploaded images

## Diagrams

Full architecture, class, sequence, deployment, and state diagrams are in `docs/architecture.md` (rendered as Mermaid — GitHub renders them automatically).
