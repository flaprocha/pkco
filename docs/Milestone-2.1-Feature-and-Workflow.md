# PKCO — Milestone 2.1: Feature & Workflow Document

October 9–10, 2026 · Group A (Sabrina Martins, Felipe Rocha)

## What changed from Milestone 2

Milestone 2.1 keeps Milestone 2 as the baseline and records eight changes agreed on October 9–10, 2026. Phase 1 stays text-only: nothing from Phase 2 or Phase 3 is built in this class project.

| # | Change | Milestone 2 said | Milestone 2.1 |
| --- | --- | --- | --- |
| 1 | Edit and delete | Not described | Phase 1 feature with its own flow (see User workflow) |
| 2 | Categories and tags | "Tags and folders/categories", model unclear | One optional category per note, plus any number of free tags |
| 3 | Persistence | "SQLite" and "Room" both named | Room over SQLite, in Phase 1 |
| 4 | Search order | "Sorted by recency and relevance", not defined | Relevance ordering defined and built in Phase 1 (see Phase 1 data model and search rule) |
| 5 | Authentication | Mentioned only inside the workflow | Explicit Phase 2 feature, required for cloud sync |
| 6 | Technology stack | Not stated | Kotlin, Jetpack Compose, Room, minimum Android 16 |
| 7 | Development and testing | Not stated | Windows 11 with Android Studio, tested on a physical Android 16 phone |
| 8 | Gaps found in review | Not covered | Backlog with a proposed phase for each item (see Backlog) |

## Feature breakdown by phase

Phase 1 delivers seven features that let a user capture, organize, edit, find and keep text notes offline; everything in Phases 2 and 3 is designed for but not built.

| Feature | Phase | What it does |
| --- | --- | --- |
| Create note | 1 | Title, body text and pasted links, saved with a floating "+" button |
| Edit note | 1 | Open a saved note, change any field, save |
| Delete note | 1 | Remove a note after a confirmation step |
| Category | 1 | One optional category per note; create, rename and delete categories (deleting leaves its notes uncategorized) |
| Tags | 1 | Any number of free tags per note, created while editing |
| Local storage with Room | 1 | All notes, categories and tags kept on the phone, available offline |
| Search with relevance order | 1 | Text search over title, body, tags and category; results ordered by relevance, then recency; list can be filtered by category or tag |
| Authentication | 2 | Sign-in so each user's notes sync to their own cloud account |
| Cloud sync and multi-device access | 2 | Notes and metadata synced to a cloud backend |
| AI text and image extraction | 2 | OCR on screenshots and photos, with AI-suggested categories and tags, and a "just save, or add context?" prompt after capture |
| Automatic timestamps and geolocation | 2 | Date, time and place attached to a note or photo when saved |
| Voice-to-text notes | 3 | Record a memo and transcribe it into a searchable note |
| Web clipper extension | 3 | Save articles and web snippets from the browser |
| AI summaries and flashcards | 3 | Generate study aids from long notes |
| Items from the review backlog | 3 | See the Backlog section for the proposed phase of each item |

## Why each item sits in its phase

Phase 1 holds only what the app needs to work on its own, with no accounts, network or external services.

- **Phase 1:** capture, edit, delete, organize and search are the smallest set that fulfils the purpose of offloading memory. Edit and delete were added because a notes app that cannot correct or remove a note is not usable. Relevance ordering answers the core pain point of not finding information.
- **Phase 2:** each feature depends on something outside the phone: an OCR or AI service, a cloud backend, or location permissions. They come after the MVP is stable.
- **Authentication moved to Phase 2** because cloud sync must know whose notes belong to whom. Phase 1 has no cloud and no accounts, so a login would add work with nothing to protect.
- **Phase 3:** voice notes, the web clipper and flashcards add convenience and build on Phase 2 services such as AI transcription and sync.

## Phase 1 design choices that prepare for Phase 2

Six design choices in Phase 1 let Phase 2 be added without rebuilding the app.

| Phase 2 need | Preparation in Phase 1 |
| --- | --- |
| Swap local storage for cloud sync | UI talks only to a repository interface; Room is the first implementation, a cloud-backed one is added later |
| Merge local and cloud records | Every record has a UUID as primary key instead of an auto-increment number |
| Detect changes and deletions | Every note has `createdAt`, `updatedAt` and a nullable `deletedAt` (UTC) |
| Authentication and per-user data | A nullable `ownerId` column on notes, unused in Phase 1 |
| Images, OCR text, location | Nullable `sourceImageUri`, `extractedText`, `latitude` and `longitude` columns, unused in Phase 1 |
| Safe schema upgrades | Versioned Room database with exported schema and migrations; no destructive fallback |

## Phase 1 data model and search rule

Four Room tables hold everything in Phase 1: notes, categories, tags, and a link table between notes and tags.

| Table | Key fields |
| --- | --- |
| Note | `id` (UUID), `title`, `body`, `categoryId` (nullable, one per note), `createdAt`, `updatedAt`, `deletedAt`, plus the reserved columns from the previous section |
| Category | `id` (UUID), `name` (unique) |
| Tag | `id` (UUID), `name` (unique, not case-sensitive) |
| NoteTag | `noteId`, `tagId` (a note can have many tags; a tag can be on many notes) |

**Relevance rule.** Each word of the query is matched, ignoring case and allowing partial matches, and the points are added up: a title match scores 3, a tag or category name match scores 2, and a body match scores 1. Results are sorted by score, then by most recently edited. This runs as one Room query, with no extra library.

## User workflow

In Phase 1 a user opens the app, writes notes, organizes them, and gets them back by browsing or searching; Phase 2 adds sign-in, sync and image capture around the same flow.

### Phase 1 (built in this project)

1. **Launch.** The app opens on the notes list, newest first, with no login. An empty list shows a hint to tap "+".
2. **Create.** The user taps "+", enters a title and body or pastes a link, optionally picks a category (or creates one), adds tags by typing them, and taps Save. Save is enabled once the title or body has text.
3. **Browse.** The list can be filtered by one category or one tag.
4. **Search.** The user types a keyword in the search bar; results appear in relevance order, then recency. No match shows a clear "nothing found" message.
5. **Edit.** Tapping a note opens it; Edit lets the user change the title, body, category and tags, and Save updates the edited time.
6. **Delete.** From the open note, Delete asks for confirmation and then removes the note from the list.
7. **Manage categories.** From the menu the user can create, rename or delete categories; deleting one leaves its notes uncategorized.

### Phase 2 (designed for, not built)

1. **Sign in** on first launch; notes then sync in the background to the user's cloud account.
2. **Capture an image.** The user takes a photo or picks a screenshot; OCR extracts the text and AI suggests a category and tags.
3. **Choose what to do with it.** The app asks "just save, or add context?" and attaches time and place automatically.
4. **Retrieve.** Search runs on the phone first and syncs changes across devices; how edit conflicts are resolved is left to the Phase 2 design.

## Technical decisions

The app is a native Android app written in Kotlin with Jetpack Compose and Room, built for Android 16 (API 36) and above.

| Area | Decision |
| --- | --- |
| Language and UI | Kotlin, Jetpack Compose |
| Storage | Room over SQLite, behind a repository interface |
| Minimum Android version | Android 16 (API 36); the app installs only on Android 16 phones |
| Development machine | Windows 11 with Android Studio (to be installed) and its bundled JDK; the system JDK 25.0.1 is not relied on |
| Test device | Physical Android 16 phone with Developer options and USB debugging on |
| Automated tests | Unit tests for the repository and the search scoring rule |
| Source control | Private GitHub repository `flaprocha/pkco` |
| AI-assisted workflow | Claude plans each sprint and Claude Code implements it; AI use and outside code are recorded in the sprint log and README |

## Backlog: gaps found while reviewing the documents

These seven items were not in the original plan; they were identified, discussed and listed while reviewing Milestones 1 and 2 and the focus-group transcript. The phase for each is a proposal.

| Item | Found in | Proposed phase |
| --- | --- | --- |
| "Just save, or add context?" prompt after a screenshot | Meeting transcript | 2 (part of AI extraction) |
| Status on items: wishlist, to do, in progress, done; read and unread | Meeting transcript | 3 |
| Events and places as item types | Meeting transcript | 3 |
| Goals with time horizons, such as 20 books this year | Meeting transcript | 3 |
| Certification suggestions and where to take them | Meeting transcript | 3 |
| Import or consolidate from Notion, Trello and similar apps | Meeting transcript | 3 |
| Nested folders | Review of Milestone 2 | 3 |

Milestone 1 listed image-to-text in the MVP, while Milestone 2 places it in Phase 2. Milestone 2 governs, so Phase 1 contains no image features.

**Decision (October 10, 2026):** database encryption is not part of Phase 1; it can be revisited in Phase 2 if the course requires it.

The living backlog, with an ID and a status for each item, is kept in [Backlog.md](Backlog.md).
