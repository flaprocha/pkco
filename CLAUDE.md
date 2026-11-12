# PKCO — guide for Claude Code

PKCO (Personal Knowledge and Context Organizer) is a native Android notes app for the Database Security course (Group A). Work is split into 8 sprints; see `docs/Sprint-Plan.md`. `docs/Milestone-2.1-Feature-and-Workflow.md` is the scope baseline. Do not edit `docs/` or `README.md` unless a sprint prompt says so.

## Stack and versions
- Kotlin (AGP 9 built-in Kotlin; Compose compiler plugin 2.4.21), Jetpack Compose, Material 3
- Android Gradle Plugin 9.4.1, Gradle 9.8.1 (wrapper), KSP 2.3.12
- Compose BOM 2026.06.01, activity-compose 1.13.0, lifecycle-runtime-ktx, lifecycle-runtime-compose and lifecycle-viewmodel-compose 2.10.0
- Room 2.8.5 (runtime, ktx, compiler via KSP)
- minSdk = compileSdk = targetSdk = 36 (Android 16). Newer Compose (1.12+) and Lifecycle (2.11+) need compileSdk 37, so they are held back.
- Versions live in `gradle/libs.versions.toml`. Single module `app`, package `com.example.pkco`.
- Application ID (Sprint 10): `io.github.flaprocha.pkco`. `namespace` and every Kotlin package stay `com.example.pkco` (R class, schema folder `app/schemas/com.example.pkco.data.PkcoDatabase/`, test packages); never package-rename. The installed app is `io.github.flaprocha.pkco`; launch component `io.github.flaprocha.pkco/com.example.pkco.MainActivity`.

## Phase 1 scope (the only phase built; complete after Sprint 8)
Text notes only, fully offline: create, edit, delete (with confirmation), one optional category per note, free tags, Room storage, search ordered by relevance, filter the list by one category or one tag.
Out of scope: Phase 2 (authentication, cloud sync, OCR/AI extraction, automatic timestamps and geolocation) and Phase 3 (voice notes, web clipper, AI summaries/flashcards, backlog items in `docs/Backlog.md`). No INTERNET permission, no network code, no database encryption in Phase 1.

## Data model (Milestone 2.1)
- Note: `id` (UUID), `title`, `body`, `categoryId` (nullable, one per note), `createdAt`, `updatedAt`, all UTC. `deletedAt` (nullable) is reserved and unused since Sprint 9: delete is permanent.
- Reserved nullable columns on Note, unused in Phase 1: `ownerId`, `sourceImageUri`, `extractedText`, `latitude`, `longitude`.
- Category: `id` (UUID), `name` (unique). Deleting a category leaves its notes uncategorized.
- Tag: `id` (UUID), `name` (unique, case-insensitive). NoteTag: `noteId`, `tagId` (many-to-many).
- UUID primary keys everywhere, no auto-increment. UI talks only to a repository interface; Room is the first implementation.
- Versioned Room database with exported schema and migrations; no destructive fallback.
- Backup and transfer: `android:allowBackup="false"` and `res/xml/data_extraction_rules.xml` (referenced by `android:dataExtractionRules`) excludes all app data (root, file, database, sharedpref, external) from both `cloud-backup` and `device-transfer`.

## Data layer (Sprint 2)
- Package `com.example.pkco.data`: `entity/` (Note, Category, Tag, NoteTag, NoteWithDetails), `dao/`, `PkcoDatabase` (version 1, `getInstance(context)`), `repository/` (`NoteRepository` interface, `RoomNoteRepository` with an injectable clock).
- Tables `notes`, `categories`, `tags`, `note_tags`. Category and tag names use NOCASE collation with a unique index.
- Delete is permanent (Sprint 9, a deviation from Milestone 2.1's soft delete): `deleteNote` removes the note's `note_tags` rows and the note row in one transaction. Unused tags are kept. Read queries still filter `deletedAt IS NULL`.
- Purge: rows soft-deleted by earlier versions (`deletedAt` set) are erased with their `note_tags` rows, in one transaction, each time the database opens (`PkcoDatabase` callback `PurgeSoftDeletedOnOpen`, same SQL as `NoteDao.purgeSoftDeleted`). No schema change.
- Schema JSON is exported to `app/schemas/` (KSP arg `room.schemaLocation`) and committed. Any schema change needs a version bump and a migration.
- Tests: `app/src/androidTest/.../data/RoomNoteRepositoryTest.kt`, in-memory database, run with `connectedDebugAndroidTest`.

## UI (Sprints 3 to 5)
- Package `com.example.pkco.ui.notes`: `NotesViewModel`, `NotesApp`, `NoteListScreen`, `NoteViewScreen`, `NoteFormScreen` (shared by create and edit), `CategoryDropdown`, `TagEditor`, `CategoryNameDialog`, `CategoriesScreen`, `NoteLabels`; theme in `ui/theme/`. `MainActivity` calls `setRecentsScreenshotEnabled(false)` (the recents screen shows no note content; normal screenshots and no FLAG_SECURE) and sets `PkcoTheme { NotesApp() }`.
- `NotesViewModel.Factory` (a `ViewModelProvider.Factory`) builds `RoomNoteRepository(PkcoDatabase.getInstance(application))`. No DI library.
- No navigation library: the sealed `Screen` (`List`, `Create`, `View(noteId)`, `Edit(noteId)`, `Categories`) is a StateFlow in the ViewModel; `NotesApp` switches on it. `selectedNote` observes the View/Edit note through `observeNote` and returns to the list when it is gone.
- Back (BackHandler): Edit -> View, View -> List, Create -> List, Categories -> List. Leaving a form discards the draft, which lives in `rememberSaveable` state. Delete asks in an AlertDialog ("permanently deleted and cannot be recovered"), then deletes permanently.
- Form: title, category dropdown (No category, each category, "New category…" opens a dialog that creates and selects it), tag field with Add (or Done) and removable chips (trimmed, no blanks, no duplicates ignoring case), body. An edit that changes neither trimmed text, category nor tag set (ignoring case) writes nothing; otherwise `updateNote` gets the chosen category and tags (it replaces both).
- Categories screen (top-bar "Categories" on the list): add field, rows with Rename (dialog) and Delete (confirmation; notes become uncategorized). Rejected names (repository `IllegalArgumentException`) show a string-resource message.
- Cards show one line "Category  #tag #tag"; View shows "Category: …" and "Tags: …" under the title. Both update live from the repository Flows. Unused tags are never shown.
- Flows are collected with `collectAsStateWithLifecycle`. Every visible text is a string resource; no Material icons library (the "+" is text).

## Relevance rule
Each query word is matched case-insensitively with partial matches; points add up: title match 3, tag or category name match 2, body match 1. Sort by score, then by most recently edited (`updatedAt`). One Room query, no extra library.

## Search (Sprint 6)
- `data/search/SearchQueryBuilder.kt` (pure Kotlin) builds the SQL and bind arguments; `NoteDao.search` (`@RawQuery`, `@Transaction`, observes all four tables) runs it; `NoteRepository.searchNotes(query, categoryId, tagId)` exposes it. No schema change.
- Rule: up to 8 whitespace-separated words, each scoring title 3 + (any tag or category name) 2 + body 1, partial LIKE match ignoring case (ASCII only), `%` `_` `\` escaped; score 0 hidden; order score, updatedAt, createdAt. Blank query = all notes in list order. User text is only ever a bind argument.
- Filters: optional category and tag (AND with each other and the words). `NotesViewModel` keeps search text, category filter and tag filter across screens; the tag menu lists only tags used by non-deleted notes. A filter whose category was deleted or whose tag is no longer used resets to "All" automatically; "Clear" resets text and both filters.

## Quality (Sprint 7)
- Errors: `NotesViewModel` catches failed writes (never `CancellationException`) in save new/edit, delete note, create/rename/delete category, and exposes a one-shot `error: StateFlow<UserError?>` (`SaveNote`, `Delete`, `SaveCategory`). `NotesApp` maps it to a string resource, calls `errorShown()` and shows a Snackbar over every screen. On failure the screen, the draft and open dialogs stay; `saving` resets. Rejected category names (`IllegalArgumentException`) still show inline.
- `saving` also guards delete (double taps ignored); Cancel and Back are ignored while a save runs; a finished save or delete only navigates if the user is still on that screen.
- Limits in `InputLimits` (cut off while typing): title 200 (counter "n / 200" always shown), body 20,000 (counter above 90%), tag 40, category name 60, 20 tags per note (Add then shows "Maximum 20 tags").
- Loading: `CenteredProgress` while the list, Categories, View or Edit has not loaded. A selected category deleted while a form is open falls back to "No category".
- Rotation: screen, search text and filters live in the ViewModel; form fields, tag field, chips, category and dialogs use `rememberSaveable`; the list's `LazyListState` is hoisted into `NotesApp` (kept across rotation and opening a note). No `configChanges`.
- Tests: `ui/notes/NoteFlowUiTest` (Compose UI test, `v2.createAndroidComposeRule<MainActivity>`, runs on the phone against the real database, unique timestamped names, removes its rows in `@After`) and `ui/notes/NotesViewModelErrorTest` (fake repository whose writes throw). Form fields have test tags in `FormTestTags`. Libraries: `ui-test-junit4` (androidTest) and `ui-test-manifest` (debug) from the Compose BOM.

## Sprint 11
- Landscape form: `NoteFormScreen` checks `LocalConfiguration` orientation. Portrait is unchanged (Body field `weight(1f)`, scrolls inside itself). Landscape: the form `Column` gets `verticalScroll` (after `imePadding`) and the Body field `heightIn(min = LANDSCAPE_BODY_MIN_HEIGHT)` (160 dp), growing with its text. Test: `ui/notes/NoteFormLandscapeUiTest` (forces landscape via `requestedOrientation`, resets it in `@After`, saves nothing).
- Read errors: every Flow the UI collects (notes, search results, categories, tag menu via notes, selected note) is wrapped in `NotesViewModel.catchRead` (rethrows `CancellationException`, sets `readFailed`, emits nothing more) and restarted by `retryReads()` through the `readAttempt` counter. `NotesApp` shows `ReadErrorScreen` ("Could not load your notes. Please try again." and "Try again") instead of every screen while `readFailed` is true. No exception text shown, no Log calls. Write errors (Snackbar) unchanged. Test: `ui/notes/NotesViewModelReadErrorTest`.
- Privacy policy: `docs/index.md` (served by GitHub Pages from `/docs` of main at https://flaprocha.github.io/pkco/). Keep it true for the merged release manifest, the code and the build files; update it before releasing any version that collects data.

## Known issues
- Process death (Android killing the app in the background) reopens on the list; drafts, dialogs, search text and filters are lost.
- Case-insensitive matching (names, tags, search) covers only ASCII letters (SQLite NOCASE and LIKE).
- Links in note bodies are plain text, not clickable.
- No undo for deleting a note or a category.
- A read failure replaces every screen with the read-error screen, so an unsaved form draft is lost.
- A note deleted while its Edit screen is open (only possible from outside this UI) returns to the list without a message; the draft is lost.
- A category created from the form's "New category…" stays even if the note is then cancelled.
- The error Snackbar is shown once; rotating while it is visible removes it.
- Dropdown menus (category picker, list filters) close on rotation; only dialogs are kept.
- Limits apply while typing only: a longer title or body saved before Sprint 7 is cut to the limit at the first keystroke in the Edit form.
- Touch targets rely on Material 3's 48 dp minimum interactive size; not checked with an accessibility scanner.

## Build, test, install (PowerShell, repo root)
```powershell
$env:JAVA_HOME = '<Android Studio folder>\jbr'   # session only
$env:ANDROID_HOME = '<Android SDK folder>'        # session only
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest   # instrumented tests, phone connected and unlocked
.\gradlew.bat installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n io.github.flaprocha.pkco/com.example.pkco.MainActivity
```
`connectedDebugAndroidTest` uninstalls the app and wipes its data, so run `installDebug` afterwards.

Release signing (Sprint 10): `signingConfigs.release` reads four Gradle properties via `providers.gradleProperty`: `PKCO_UPLOAD_STORE_FILE`, `PKCO_UPLOAD_STORE_PASSWORD`, `PKCO_UPLOAD_KEY_ALIAS`, `PKCO_UPLOAD_KEY_PASSWORD`. Set them only in the user-level `~/.gradle/gradle.properties` (or `$GRADLE_USER_HOME/gradle.properties`) or with `-P`, never in the repo. With all four set, the `release` build type is signed; with any missing, debug builds and tests are unaffected and `packageRelease` / `signReleaseBundle` fail with a message naming them. `isMinifyEnabled = false`. Claude Code never reads, creates, prints or commits a keystore or password and does not run release builds with real keys.

JVM unit tests (`.\gradlew.bat testDebugUnitTest`, under `app/src/test/`) cover only pure Kotlin code such as the search query builder; Room tests need a device (Robolectric is not used).
If drive C: is low on space, also set `GRADLE_USER_HOME`, `TMP` and `TEMP` to folders on another drive for the session (done in Sprint 1). A full C: also stops Windows' swap file from growing, and the Gradle JVM then crashes with "insufficient memory"; free space on C: and close other programs, or pass `-Dorg.gradle.jvmargs=-Xmx1536m` for the session.
Test phone: Motorola moto g stylus 2025, Android 16.

## JDK note
The machine's system JDK is 25 and is not relied on. Run Gradle with Android Studio's bundled `jbr` (currently JDK 25.0.3, which this AGP/Gradle accepts), set per session or in the user-level `~/.gradle/gradle.properties`, never in the repo. `local.properties` holds `sdk.dir` and is git-ignored.

## Sprint log times
For every sprint: (a) your very first command is `Get-Date -Format 'HH:mm:ss'`; record it as Start. (b) Just before the commit run it again and record it as Code finished. (c) Copy the duration Gradle prints for connectedDebugAndroidTest into 'Automated tests' (for example 'BUILD SUCCESSFUL in 1m 12s'). (d) Copy 'Manual test (expected)' from the sprint prompt. (e) End is added by Felipe as HH:MM:SS; write 'End: to be added by Felipe (HH:MM:SS from the computer clock)'. Time cell format, one line each joined with <br>: <date><br>Start: HH:MM:SS<br>Code finished: HH:MM:SS<br>Automated tests: <duration><br>Manual test (expected): ~N min<br>End: ... Never invent a time; every time is HH:MM:SS.

## Working rules
- Stay inside the current sprint's scope; do not build later-sprint or Phase 2/3 features.
- Report every deviation from the sprint prompt, and anything skipped.
- Add no dependencies, plugins or permissions without asking first.
- Never commit local paths, `local.properties`, keystores or secrets.
- Commit only as the sprint prompt says. Never push.
- Do not install software, accept SDK licenses, or change permanent system settings; stop and report instead.
