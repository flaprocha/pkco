# PKCO — guide for Claude Code

PKCO (Personal Knowledge and Context Organizer) is a native Android notes app for the Database Security course (Group A). Work is split into 8 sprints; see `docs/Sprint-Plan.md`. `docs/Milestone-2.1-Feature-and-Workflow.md` is the scope baseline. Do not edit `docs/` or `README.md` unless a sprint prompt says so.

## Stack and versions
- Kotlin (AGP 9 built-in Kotlin; Compose compiler plugin 2.4.21), Jetpack Compose, Material 3
- Android Gradle Plugin 9.4.1, Gradle 9.8.1 (wrapper), KSP 2.3.12
- Compose BOM 2026.06.01, activity-compose 1.13.0, lifecycle-runtime-ktx, lifecycle-runtime-compose and lifecycle-viewmodel-compose 2.10.0
- Room 2.8.5 (runtime, ktx, compiler via KSP)
- minSdk = compileSdk = targetSdk = 36 (Android 16). Newer Compose (1.12+) and Lifecycle (2.11+) need compileSdk 37, so they are held back.
- Versions live in `gradle/libs.versions.toml`. Single module `app`, package `com.example.pkco`.

## Phase 1 scope (the only phase built)
Text notes only, fully offline: create, edit, delete (with confirmation), one optional category per note, free tags, Room storage, search ordered by relevance, filter the list by one category or one tag.
Out of scope: Phase 2 (authentication, cloud sync, OCR/AI extraction, automatic timestamps and geolocation) and Phase 3 (voice notes, web clipper, AI summaries/flashcards, backlog items in `docs/Backlog.md`). No INTERNET permission, no network code, no database encryption in Phase 1.

## Data model (Milestone 2.1)
- Note: `id` (UUID), `title`, `body`, `categoryId` (nullable, one per note), `createdAt`, `updatedAt`, `deletedAt` (nullable), all UTC.
- Reserved nullable columns on Note, unused in Phase 1: `ownerId`, `sourceImageUri`, `extractedText`, `latitude`, `longitude`.
- Category: `id` (UUID), `name` (unique). Deleting a category leaves its notes uncategorized.
- Tag: `id` (UUID), `name` (unique, case-insensitive). NoteTag: `noteId`, `tagId` (many-to-many).
- UUID primary keys everywhere, no auto-increment. UI talks only to a repository interface; Room is the first implementation.
- Versioned Room database with exported schema and migrations; no destructive fallback.

## Data layer (Sprint 2)
- Package `com.example.pkco.data`: `entity/` (Note, Category, Tag, NoteTag, NoteWithDetails), `dao/`, `PkcoDatabase` (version 1, `getInstance(context)`), `repository/` (`NoteRepository` interface, `RoomNoteRepository` with an injectable clock).
- Tables `notes`, `categories`, `tags`, `note_tags`. Category and tag names use NOCASE collation with a unique index.
- Notes are soft-deleted (`deletedAt` set); read queries filter `deletedAt IS NULL`. Unused tags are kept.
- Schema JSON is exported to `app/schemas/` (KSP arg `room.schemaLocation`) and committed. Any schema change needs a version bump and a migration.
- Tests: `app/src/androidTest/.../data/RoomNoteRepositoryTest.kt`, in-memory database, run with `connectedDebugAndroidTest`.

## UI (Sprint 3)
- Package `com.example.pkco.ui.notes`: `NotesViewModel`, `NotesApp`, `NoteListScreen`, `CreateNoteScreen`; theme in `ui/theme/`. `MainActivity` only sets `PkcoTheme { NotesApp() }`.
- `NotesViewModel.Factory` (a `ViewModelProvider.Factory`) builds `RoomNoteRepository(PkcoDatabase.getInstance(application))`. No DI library.
- No navigation library: the `Screen` enum (`List`, `Create`) is a StateFlow in the ViewModel; `NotesApp` switches on it. Back on the create screen returns to the list (BackHandler) and discards the draft, which lives in `rememberSaveable` state.
- Flows are collected with `collectAsStateWithLifecycle`. Every visible text is a string resource; no Material icons library (the "+" is text).

## Relevance rule
Each query word is matched case-insensitively with partial matches; points add up: title match 3, tag or category name match 2, body match 1. Sort by score, then by most recently edited (`updatedAt`). One Room query, no extra library.

## Build, test, install (PowerShell, repo root)
```powershell
$env:JAVA_HOME = '<Android Studio folder>\jbr'   # session only
$env:ANDROID_HOME = '<Android SDK folder>'        # session only
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest   # instrumented tests, phone connected and unlocked
.\gradlew.bat installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.example.pkco/.MainActivity
```
There are no JVM unit tests (`testDebugUnitTest`) at the moment: Room tests need a device, and Robolectric is not used.
If drive C: is low on space, also set `GRADLE_USER_HOME`, `TMP` and `TEMP` to folders on another drive for the session (done in Sprint 1). A full C: also stops Windows' swap file from growing, and the Gradle JVM then crashes with "insufficient memory"; free space on C: and close other programs, or pass `-Dorg.gradle.jvmargs=-Xmx1536m` for the session.
Test phone: Motorola moto g stylus 2025, Android 16.

## JDK note
The machine's system JDK is 25 and is not relied on. Run Gradle with Android Studio's bundled `jbr` (currently JDK 25.0.3, which this AGP/Gradle accepts), set per session or in the user-level `~/.gradle/gradle.properties`, never in the repo. `local.properties` holds `sdk.dir` and is git-ignored.

## Working rules
- Stay inside the current sprint's scope; do not build later-sprint or Phase 2/3 features.
- Report every deviation from the sprint prompt, and anything skipped.
- Add no dependencies, plugins or permissions without asking first.
- Never commit local paths, `local.properties`, keystores or secrets.
- Commit only as the sprint prompt says. Never push.
- Do not install software, accept SDK licenses, or change permanent system settings; stop and report instead.
