# Last War ANO 2193

Alliance ranking manager for the Last War mobile game.
Upload screenshots of in-game ranking tables; OCR extracts player entries automatically.
Entries can be manually reviewed, corrected, and approved before they are included in the event ranking.

---

## Quick start

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).
Default credentials: `admin / admin123`, `submitter / sub123`, `viewer / view123`.

---

## OCR strategies

The OCR layer is pluggable. Set `ocr.strategy` in `application.properties` or via the
`OCR_STRATEGY` environment variable:

| Value | Description |
|---|---|
| `tesseract` | Local Tesseract engine via tess4j. Requires `tesseract-ocr` installed on the host. |
| `vision-llm` | OpenAI-compatible vision endpoint (Ollama, GPT-4o, …). Best accuracy. |
| `mock` | Always returns hardcoded text. Useful for UI development without an OCR engine. |

---

## Setting up the Vision LLM (Ollama)

Ollama exposes an OpenAI-compatible API, so the same strategy class works for both
a self-hosted Ollama instance and the OpenAI cloud API.

### Option A — Docker Compose (recommended)

The `docker-compose.yml` already includes an `ollama` service.
Start the stack, then pull a vision model once:

```bash
# Start everything
docker compose up -d

# Pull a vision model (runs inside the Ollama container)
docker compose exec ollama ollama pull llava:13b

# Switch the app to use it
# Either set the env var in a .env file (see below) or edit application.properties
```

To switch the app to the vision-llm strategy, add a `.env` file in the project root:

```env
OCR_STRATEGY=vision-llm
OCR_VISION_BASE_URL=http://ollama:11434
OCR_VISION_MODEL=llava:13b
```

Then restart:

```bash
docker compose up -d --force-recreate app
```

#### GPU support (NVIDIA)

Uncomment the `deploy.resources` block in `docker-compose.yml` for the `ollama` service
and ensure the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) is installed on the host.

#### Available vision models

| Model | Size | Notes |
|---|---|---|
| `llava:7b` | ~4 GB | Faster, lower accuracy |
| `llava:13b` | ~8 GB | Good balance — recommended |
| `llava:34b` | ~20 GB | Highest accuracy, requires GPU |
| `moondream` | ~1.7 GB | Very fast, lower accuracy |

---

### Option B — Ollama standalone (without Docker Compose)

Run Ollama as a standalone container, then point the app at it:

```bash
# Start Ollama (CPU)
docker run -d \
  --name ollama \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  ollama/ollama

# Start Ollama (NVIDIA GPU)
docker run -d \
  --name ollama \
  --gpus all \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  ollama/ollama

# Pull a vision model
docker exec -it ollama ollama pull llava:13b

# Verify
curl http://localhost:11434/api/tags
```

---

### Option C — OpenAI GPT-4o

No local process needed. Requires an OpenAI API key.

In `application.properties`:

```properties
ocr.strategy=vision-llm
ocr.vision.base-url=https://api.openai.com
ocr.vision.model=gpt-4o
ocr.vision.api-key=sk-...
```

Or via environment variables (see below).

---

## Configuring via environment variables

All OCR settings can be overridden with environment variables — useful for IntelliJ run
configurations, CI, or Docker deployments without editing `application.properties`.

Spring Boot maps `SOME_PROPERTY_NAME` → `some.property.name` automatically.

| Environment variable | Property | Default |
|---|---|---|
| `OCR_STRATEGY` | `ocr.strategy` | `tesseract` |
| `OCR_VISION_BASE_URL` | `ocr.vision.base-url` | `http://localhost:11434` |
| `OCR_VISION_MODEL` | `ocr.vision.model` | `llava:13b` |
| `OCR_VISION_API_KEY` | `ocr.vision.api-key` | *(empty)* |
| `OCR_VISION_TIMEOUT_SECONDS` | `ocr.vision.timeout-seconds` | `60` |

### IntelliJ IDEA run configuration

1. Open **Run → Edit Configurations…**
2. Select the `LastWarApplication` configuration (or create one).
3. Expand **Environment variables** and add:

```
OCR_STRATEGY=vision-llm
OCR_VISION_BASE_URL=http://localhost:11434
OCR_VISION_MODEL=llava:13b
```

Make sure Ollama is running locally (Option B above) before starting the app.

### .env file for Docker Compose

Create `.env` in the project root (it is git-ignored):

```env
OCR_STRATEGY=vision-llm
OCR_VISION_BASE_URL=http://ollama:11434
OCR_VISION_MODEL=llava:13b
OCR_VISION_API_KEY=
```

`docker compose` picks this up automatically.

---

## Architecture

See `docs/architecture.md` for full Mermaid diagrams (rendered on GitHub).

## Database

H2 file-mode database at `./data/appdb.mv.db`. H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
(JDBC URL: `jdbc:h2:file:./data/appdb`). Disabled in the Docker Compose deployment.