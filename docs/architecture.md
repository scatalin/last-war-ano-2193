# Last War Alliance Manager (Anno 2193) — Architecture Diagrams

These diagrams are written in [Mermaid](https://mermaid.js.org/). They render
automatically on GitHub, in IntelliJ IDEA (with the *Mermaid* plugin), and in
VS Code (with *Markdown Preview Mermaid Support*). See
[How to render / regenerate](#how-to-render--regenerate) at the bottom.

- [1. Layered architecture overview](#1-layered-architecture-overview)
- [2. Class diagram (whole application)](#2-class-diagram-whole-application)
- [3. Sequence diagrams (per controller endpoint)](#3-sequence-diagrams-per-controller-endpoint)

---

## 1. Layered architecture overview

```mermaid
flowchart TB
    subgraph client["Client"]
        browser["Browser"]
    end

    subgraph web["Web / Presentation Layer"]
        sec["SecurityFilterChain<br/>(form login, role rules, CSRF)"]
        tmpl["Thymeleaf Templates<br/>dashboard · rankings · upload<br/>login · admin/users · admin/data"]
        subgraph controllers["Controllers (@Controller)"]
            dash["DashboardController"]
            rank["RankingController"]
            photo["PhotoController"]
            admin["AdminController"]
        end
    end

    subgraph service["Service Layer (@Service)"]
        rsvc["RankingService"]
        usvc["UserService<br/>(implements UserDetailsService)"]
        csvc["CsvService"]
        isvc["ImageParsingService"]
    end

    subgraph data["Persistence Layer"]
        subgraph repos["Repositories (Spring Data JPA)"]
            rrepo["RankingEntryRepository"]
            urepo["UserRepository"]
            prepo["PhotoUploadRepository"]
        end
        db[("H2 Database<br/>(in-memory)")]
    end

    subgraph infra["Cross-cutting / Infrastructure"]
        init["DataInitializer<br/>(seeds default users)"]
        sched["CsvPersistenceScheduler<br/>(@Scheduled, every 5 min)"]
        fs[["File System<br/>./uploads · ./data/rankings.csv"]]
    end

    browser -->|HTTP| sec
    sec --> controllers
    controllers -->|render| tmpl
    tmpl -->|HTML| browser

    dash --> rsvc
    rank --> rsvc
    photo --> isvc
    photo --> rsvc
    photo --> csvc
    photo --> prepo
    admin --> usvc
    admin --> rsvc
    admin --> csvc

    rsvc --> rrepo
    usvc --> urepo
    csvc --> rrepo
    csvc -.read/write.-> fs
    isvc -.reads.-> fs
    photo -.stores image.-> fs

    rrepo --> db
    urepo --> db
    prepo --> db

    init --> usvc
    sched --> csvc

    sec -.authenticates via.-> usvc
```

**Layer responsibilities**

| Layer | Components | Responsibility |
|-------|-----------|----------------|
| Web | `*Controller`, `SecurityConfig`, Thymeleaf templates | HTTP routing, auth rules, view rendering |
| Service | `RankingService`, `UserService`, `CsvService`, `ImageParsingService` | Business logic, transactions, OCR/CSV orchestration |
| Persistence | `*Repository`, H2 | CRUD + derived queries, storage |
| Infra | `DataInitializer`, `CsvPersistenceScheduler`, file system | Bootstrapping, scheduled CSV export, blob/CSV storage |

---

## 2. Class diagram (whole application)

```mermaid
classDiagram
    direction LR

    %% ───────── Models / Entities ─────────
    class User {
        -Long id
        -String username
        -String password
        -Set~String~ roles
        -boolean enabled
        +getters/setters()
    }
    class RankingEntry {
        -Long id
        -Integer rank
        -String playerName
        -String allianceTag
        -Long power
        -Long kills
        -String category
        -String sourcePhotoPath
        -String submittedBy
        -LocalDateTime capturedAt
        -Map~String,String~ metadata
        +getters/setters()
    }
    class PhotoUpload {
        -Long id
        -String filename
        -String originalFilename
        -String category
        -String uploadedBy
        -LocalDateTime uploadedAt
        -String status
        -String notes
        +getters/setters()
    }

    %% ───────── Repositories ─────────
    class JpaRepository~T,ID~ {
        <<interface>>
        +findAll() List
        +findById(ID) Optional
        +save(T) T
        +saveAll(Iterable) List
        +deleteById(ID)
        +count() long
    }
    class RankingEntryRepository {
        <<interface>>
        +findByCategory(String) List
        +findBySubmittedBy(String) List
        +findByPlayerNameContainingIgnoreCase(String) List
    }
    class UserRepository {
        <<interface>>
        +findByUsername(String) Optional
        +existsByUsername(String) boolean
    }
    class PhotoUploadRepository {
        <<interface>>
        +findByUploadedByOrderByUploadedAtDesc(String) List
        +findByStatus(String) List
    }

    %% ───────── Services ─────────
    class RankingService {
        <<Service>>
        -RankingEntryRepository repository
        +findAll() List
        +findByCategory(String) List
        +findById(Long) Optional
        +save(RankingEntry) RankingEntry
        +saveAll(List) void
        +delete(Long) void
        +count() long
        +findAllCategories() List
    }
    class UserService {
        <<Service>>
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +loadUserByUsername(String) UserDetails
        +createUser(String,String,Set) User
        +findAll() List
        +findById(Long) Optional
        +findByUsername(String) Optional
        +deleteUser(Long) void
        +existsByUsername(String) boolean
    }
    class CsvService {
        <<Service>>
        -RankingEntryRepository rankingEntryRepository
        -String dataDir
        +loadFromCsv() void
        +saveToCsv() void
        +exportRankingsToCsv() void
    }
    class ImageParsingService {
        <<Service>>
        +parseImage(File,String,String) List
        +parseOcrText(String,String,String,String) List
    }
    class UserDetailsService {
        <<interface>>
        +loadUserByUsername(String) UserDetails
    }

    %% ───────── Controllers ─────────
    class DashboardController {
        <<Controller>>
        -RankingService rankingService
        +index() String
        +login() String
        +dashboard(Model) String
    }
    class RankingController {
        <<Controller>>
        -RankingService rankingService
        +rankings(String,Model) String
    }
    class PhotoController {
        <<Controller>>
        -ImageParsingService imageParsingService
        -RankingService rankingService
        -PhotoUploadRepository photoUploadRepository
        -CsvService csvService
        -String uploadDir
        +uploadForm(Model) String
        +handleUpload(MultipartFile,String,UserDetails,RedirectAttributes) String
    }
    class AdminController {
        <<Controller>>
        -UserService userService
        -RankingService rankingService
        -CsvService csvService
        +adminHome() String
        +users(Model) String
        +createUser(...) String
        +deleteUser(Long,RedirectAttributes) String
        +data(Model) String
        +deleteEntry(Long,RedirectAttributes) String
        +exportCsv(RedirectAttributes) String
    }

    %% ───────── Config / Infra ─────────
    class SecurityConfig {
        <<Configuration>>
        +passwordEncoder() PasswordEncoder
        +authenticationProvider(UserService) DaoAuthenticationProvider
        +securityFilterChain(HttpSecurity) SecurityFilterChain
    }
    class DataInitializer {
        <<Component>>
        -UserService userService
        +run(String...) void
    }
    class CsvPersistenceScheduler {
        <<Component>>
        -CsvService csvService
        +persistToCsv() void
    }
    class LastWarApplication {
        +main(String[]) void
    }

    %% ───────── Relationships ─────────
    JpaRepository <|-- RankingEntryRepository
    JpaRepository <|-- UserRepository
    JpaRepository <|-- PhotoUploadRepository
    UserDetailsService <|.. UserService

    RankingService --> RankingEntryRepository
    UserService --> UserRepository
    CsvService --> RankingEntryRepository

    DashboardController --> RankingService
    RankingController --> RankingService
    PhotoController --> ImageParsingService
    PhotoController --> RankingService
    PhotoController --> CsvService
    PhotoController --> PhotoUploadRepository
    AdminController --> UserService
    AdminController --> RankingService
    AdminController --> CsvService

    DataInitializer --> UserService
    CsvPersistenceScheduler --> CsvService
    SecurityConfig ..> UserService

    RankingEntryRepository ..> RankingEntry
    UserRepository ..> User
    PhotoUploadRepository ..> PhotoUpload
```

---

## 3. Sequence diagrams (per controller endpoint)

Every browser request first passes through the Spring Security
`SecurityFilterChain`. Role requirements (from `SecurityConfig`) are noted per
endpoint. To keep the diagrams readable, the filter chain is shown explicitly in
the first diagram and abbreviated as a note thereafter.

### 3.1 `GET /` — DashboardController.index

`VIEWER+` · redirects to the dashboard.

```mermaid
sequenceDiagram
    actor U as Browser
    participant SF as SecurityFilterChain
    participant C as DashboardController
    U->>SF: GET /
    SF->>SF: authenticate (ROLE_VIEWER+)
    SF->>C: index()
    C-->>U: 302 redirect:/dashboard
```

### 3.2 `GET /login` — DashboardController.login

Public (permitAll).

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as DashboardController
    participant V as Thymeleaf (login.html)
    U->>C: GET /login
    C->>V: return "login"
    V-->>U: 200 login page
```

### 3.3 `GET /dashboard` — DashboardController.dashboard

`VIEWER+`.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as DashboardController
    participant RS as RankingService
    participant RR as RankingEntryRepository
    participant DB as H2
    participant V as Thymeleaf (dashboard.html)
    U->>C: GET /dashboard
    C->>RS: count()
    RS->>RR: count()
    RR->>DB: SELECT count(*)
    DB-->>C: total
    C->>RS: findAllCategories()
    RS->>RR: findAll()
    RR->>DB: SELECT *
    DB-->>RS: entries
    RS-->>C: distinct sorted categories
    C->>V: model{totalEntries, categories}
    V-->>U: 200 dashboard
```

### 3.4 `GET /rankings` — RankingController.rankings

`VIEWER+` · optional `?category=` filter.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as RankingController
    participant RS as RankingService
    participant RR as RankingEntryRepository
    participant DB as H2
    participant V as Thymeleaf (rankings.html)
    U->>C: GET /rankings?category=
    alt category provided
        C->>RS: findByCategory(category)
        RS->>RR: findByCategory(category)
        RR->>DB: SELECT ... WHERE category=?
    else no category
        C->>RS: findAll()
        RS->>RR: findAll()
        RR->>DB: SELECT *
    end
    DB-->>C: entries
    C->>RS: findAllCategories()
    RS-->>C: categories
    C->>V: model{entries, categories, selectedCategory}
    V-->>U: 200 rankings
```

### 3.5 `GET /upload` — PhotoController.uploadForm

`SUBMITTER` or `ADMIN`.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as PhotoController
    participant PR as PhotoUploadRepository
    participant DB as H2
    participant V as Thymeleaf (upload.html)
    U->>C: GET /upload
    C->>PR: findAll()
    PR->>DB: SELECT *
    DB-->>C: uploads
    C->>V: model{uploads}
    V-->>U: 200 upload form
```

### 3.6 `POST /upload` — PhotoController.handleUpload

`SUBMITTER` or `ADMIN` · file + category, runs the full ingest pipeline.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as PhotoController
    participant PR as PhotoUploadRepository
    participant FS as File System
    participant IPS as ImageParsingService
    participant RS as RankingService
    participant CS as CsvService
    participant DB as H2

    U->>C: POST /upload (file, category)
    alt file empty
        C-->>U: 302 redirect:/upload (error flash)
    else file present
        C->>C: sanitize category
        C->>PR: save(PhotoUpload status=PROCESSING)
        PR->>DB: INSERT
        C->>FS: copy file -> UUID.ext
        C->>IPS: parseImage(file, category, user)
        Note over IPS: OCR stub — returns empty list<br/>(Tesseract integration point)
        IPS-->>C: List<RankingEntry>
        C->>RS: saveAll(entries)
        RS->>DB: INSERT entries
        C->>CS: exportRankingsToCsv()
        CS->>DB: findAll()
        CS->>FS: write data/rankings.csv
        C->>PR: save(PhotoUpload status=PROCESSED)
        PR->>DB: UPDATE
        C-->>U: 302 redirect:/upload (success flash)
    end
    Note over C: on IOException -> status=FAILED, error flash
```

### 3.7 `GET /admin` — AdminController.adminHome

`ADMIN` only · redirects to user management.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    U->>C: GET /admin
    C-->>U: 302 redirect:/admin/users
```

### 3.8 `GET /admin/users` — AdminController.users

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant US as UserService
    participant UR as UserRepository
    participant DB as H2
    participant V as Thymeleaf (admin/users.html)
    U->>C: GET /admin/users
    C->>US: findAll()
    US->>UR: findAll()
    UR->>DB: SELECT *
    DB-->>C: users
    C->>V: model{users}
    V-->>U: 200 users page
```

### 3.9 `POST /admin/users/create` — AdminController.createUser

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant US as UserService
    participant PE as PasswordEncoder
    participant UR as UserRepository
    participant DB as H2
    U->>C: POST /admin/users/create (username, password, role)
    C->>US: existsByUsername(username)
    US->>UR: existsByUsername(username)
    UR->>DB: SELECT count
    alt already exists
        C-->>U: 302 redirect:/admin/users (error flash)
    else new user
        C->>C: map role -> role set
        C->>US: createUser(username, password, roles)
        US->>PE: encode(password)
        PE-->>US: hash
        US->>UR: save(user)
        UR->>DB: INSERT
        C-->>U: 302 redirect:/admin/users (success flash)
    end
```

### 3.10 `POST /admin/users/delete/{id}` — AdminController.deleteUser

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant US as UserService
    participant UR as UserRepository
    participant DB as H2
    U->>C: POST /admin/users/delete/{id}
    C->>US: deleteUser(id)
    US->>UR: deleteById(id)
    UR->>DB: DELETE
    C-->>U: 302 redirect:/admin/users (success flash)
```

### 3.11 `GET /admin/data` — AdminController.data

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant RS as RankingService
    participant RR as RankingEntryRepository
    participant DB as H2
    participant V as Thymeleaf (admin/data.html)
    U->>C: GET /admin/data
    C->>RS: findAll()
    RS->>RR: findAll()
    RR->>DB: SELECT *
    DB-->>C: entries
    C->>RS: findAllCategories()
    RS-->>C: categories
    C->>V: model{entries, categories}
    V-->>U: 200 data page
```

### 3.12 `POST /admin/data/delete/{id}` — AdminController.deleteEntry

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant RS as RankingService
    participant RR as RankingEntryRepository
    participant DB as H2
    U->>C: POST /admin/data/delete/{id}
    C->>RS: delete(id)
    RS->>RR: deleteById(id)
    RR->>DB: DELETE
    C-->>U: 302 redirect:/admin/data (success flash)
```

### 3.13 `POST /admin/data/export` — AdminController.exportCsv

`ADMIN` only.

```mermaid
sequenceDiagram
    actor U as Browser
    participant C as AdminController
    participant CS as CsvService
    participant RR as RankingEntryRepository
    participant DB as H2
    participant FS as File System
    U->>C: POST /admin/data/export
    C->>CS: exportRankingsToCsv()
    CS->>RR: findAll()
    RR->>DB: SELECT *
    DB-->>CS: entries
    CS->>FS: write data/rankings.csv
    C-->>U: 302 redirect:/admin/data (success flash)
```

---

## How to render / regenerate

**View the diagrams**
- **GitHub** — just open this `.md` file; Mermaid blocks render automatically.
- **IntelliJ IDEA** — install the *Mermaid* plugin (Settings → Plugins), then open the Markdown preview.
- **VS Code** — install *Markdown Preview Mermaid Support*, then `Ctrl+Shift+V`.

**Export to PNG/SVG/PDF** with the Mermaid CLI:
```bash
npm install -g @mermaid-js/mermaid-cli
# Render every diagram in this file to numbered PNGs:
mmdc -i docs/architecture.md -o docs/architecture.png
```

**Keep them in sync as the code changes**
- The diagrams are hand-derived from the source. After adding a controller
  endpoint, service, or entity, update the relevant block here.
- To auto-generate a class diagram from bytecode instead, consider
  [PlantUML](https://plantuml.com/) with a Java parser, or the IntelliJ
  *Diagrams → Show Diagram* feature on the `com.lastwar.ano2193` package
  (Ultimate edition) for an always-accurate UML class diagram.
