<div align="center">

# 🧠 PKCO — Personal Knowledge and Context Organizer

**One place to capture, organize and find your notes, links and study material.**

![Platform](https://img.shields.io/badge/platform-Android%2016%2B-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Storage](https://img.shields.io/badge/storage-Room%20%2F%20SQLite-lightgrey)
![Status](https://img.shields.io/badge/status-Phase%201%20complete-brightgreen)

</div>

---

## 📚 About this project

PKCO is an Android app developed to fulfill the **Group A assignment** for the **Database Security** course in Caroline University's [Master of Computer Information Systems program](https://caroline.edu/bbs/content.php?co_id=academics02_3). The course is led by **Professor Martin Lee**, our instructor and tutor, who guides the project through structured milestones.

The app helps people who juggle many tasks and study streams stop losing track of where their information lives. Instead of notes scattered across several apps, PKCO keeps ideas, links and study snippets in one searchable, organized workspace on the phone.

> *"Your mind is for having ideas, not holding them."* — David Allen

### 👥 Team — Group A

| Member |
| --- |
| Sabrina Martins |
| Felipe Rocha |

---

## ✨ Features

The project is planned in three phases. **Only Phase 1 is built for this class project**; the Phase 1 design is prepared so Phase 2 can be added later without rebuilding the app.

| Phase | Features |
| --- | --- |
| **1 · MVP (built here)** | Create, edit and delete text notes with links · one optional category per note · free tags · local storage with Room (works offline) · search ordered by relevance · filter by category and tag |
| **2 · Competitive** *(designed for, not built)* | Authentication · cloud sync across devices · OCR and AI extraction from screenshots · automatic timestamps and geolocation |
| **3 · Nice to have** *(ideas only)* | Voice-to-text notes · web clipper · AI summaries and flashcards · item status, events, goals and other backlog ideas |

### What the app does today (Phase 1)

- **Notes:** title and body, with any link pasted into the body. Open a note to read it, edit it, or delete it after a confirmation.
- **Categories:** create, rename and delete categories; each note has at most one. Deleting a category keeps its notes, which become uncategorized.
- **Tags:** add up to 20 free tags to a note and remove them again. Tags are not case-sensitive.
- **Search:** type words in the search box; results are ordered by relevance. Each word scores 3 points for a title match, 2 for a tag or category name match and 1 for a body match, partial words count, ties go to the most recently edited note.
- **Filters:** narrow the list by one category and/or one tag, alone or together with a search.
- **Quality:** works offline, keeps its data after the app is closed, keeps typed text when the screen rotates (the landscape form layout has a known bug, see Known limitations below), shows messages instead of crashing when a save fails, and limits text length (title 200, body 20,000, tag 40, category name 60 characters).

Full details are in [`docs/Milestone-2.1-Feature-and-Workflow.md`](docs/Milestone-2.1-Feature-and-Workflow.md). A guided tour for demonstrating the app is in [`docs/Demo-Script.md`](docs/Demo-Script.md).

---

## 🛠️ Technology

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose with Material 3 |
| Local database | Room over SQLite, behind a repository interface; the schema is versioned and exported to `app/schemas/` |
| Minimum Android version | Android 16 (API 36); the app installs only on Android 16 or newer |
| Build | Android Gradle Plugin 9.4.1, Gradle 9.8.1 (wrapper included), KSP |
| Development | Android Studio on Windows 11, tested on a physical Android 16 phone |
| Source control | Git and GitHub |

Exact library versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). The app has no network permission and no accounts; all data stays on the phone.

---

## 🗂️ Repository layout

```text
pkco/
├── app/
│   ├── schemas/                         Exported Room schema (version 1)
│   └── src/
│       ├── main/java/com/example/pkco/
│       │   ├── data/                    Room entities, DAOs, database, repository, search query builder
│       │   └── ui/                      Compose screens, ViewModel, theme
│       ├── test/                        JVM unit tests (search query builder)
│       └── androidTest/                 Tests that run on the phone (database, search, UI flow)
├── docs/                                Milestones, sprint plan, backlog, demo script
├── CLAUDE.md                            Project guide used by Claude Code
├── SPRINT_LOG.md                        Sprint-by-sprint record of the AI-assisted workflow
└── README.md                            You are here
```

---

## 🤖 How this project is being built

Part of this assignment is an experiment in AI-assisted development. Over eight short sprints, **Claude** planned each step and wrote the instructions, and **Claude Code** implemented them, while the team member followed the recommendations, tested every sprint by hand on the phone and logged where the AI helped, drifted or needed correction.

- Plan and sprint roadmap: [`docs/Sprint-Plan.md`](docs/Sprint-Plan.md)
- Sprint-by-sprint record with times, deviations and human interventions: [`SPRINT_LOG.md`](SPRINT_LOG.md)
- Each sprint is one commit tagged `sprint-1` to `sprint-8`. A few extra documentation commits (README, demo script, backlog, and one that corrects the Sprint 6 log entry) sit between them.

| Sprint | Goal |
| --- | --- |
| 1 | Setup and project skeleton |
| 2 | Data layer: Room tables, repository, tests |
| 3 | Notes list and create screen |
| 4 | Open, edit and delete notes |
| 5 | Categories and tags |
| 6 | Search, relevance order and filters |
| 7 | Quality: error states, input limits, rotation, UI test |
| 8 | Documentation, credits, final test pass and demo script |

AI tools are permitted in this course, and the team is responsible for being able to explain all submitted code and design decisions.

---

## 🚀 Getting started

### What you need

- A computer with **Android Studio** (a current release) and the **Android SDK Platform 36 (Android 16)** installed from the SDK Manager, plus the platform tools (`adb`).
- An **Android 16 phone** with Developer options and USB debugging switched on, connected with a data cable. An Android 16 emulator also works, but the project was only tested on a phone.
- **Git**.

### Run it from Android Studio

1. Clone the repository: `git clone https://github.com/flaprocha/pkco` (it is private; you need access).
2. In Android Studio choose **Open** and select the cloned folder. Let Gradle sync finish. Android Studio creates the `local.properties` file with your SDK location by itself.
3. Make sure the Gradle JDK is the one bundled with Android Studio (Settings → Build, Execution, Deployment → Build Tools → Gradle).
4. Pick your phone in the device list and press **Run** on the `app` configuration.

### Run it from the command line (PowerShell on Windows)

```powershell
$env:JAVA_HOME = '<Android Studio folder>\jbr'
$env:ANDROID_HOME = '<Android SDK folder>'
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.example.pkco/.MainActivity
```

If you did not use Android Studio first, create a `local.properties` file in the repository root containing `sdk.dir=<path to your Android SDK>` (with `\` doubled on Windows). The file is ignored by Git.

---

## ✅ Testing

| What | Command | Notes |
| --- | --- | --- |
| JVM unit tests | `.\gradlew.bat testDebugUnitTest` | 10 tests for the search query builder; no phone needed |
| Tests on the phone | `.\gradlew.bat connectedDebugAndroidTest` | 29 tests (final run recorded in Sprint 8): database and repository, relevance search and filters, a view-model error test and a UI flow test (create, rotate, edit, delete). The phone must be connected and unlocked |
| Manual test | [`docs/Demo-Script.md`](docs/Demo-Script.md) | About 12 minutes on a phone; also the script for the demo |

> ⚠️ `connectedDebugAndroidTest` uninstalls the app and wipes its data when it finishes. Run `installDebug` afterwards, and create your test notes after that.

The test counts above are the ones recorded in [`SPRINT_LOG.md`](SPRINT_LOG.md); the final run is recorded in the Sprint 8 row.

---

## ⚠️ Known limitations

- **Landscape form bug (found in the final manual test):** when the phone is rotated to landscape on the create or edit form, the Body field becomes too small to read and the form cannot be scrolled, so typed text cannot be seen or checked (the text is kept and saved). Portrait works. Not fixed: the last sprint was a freeze on code. See B-20 in [`docs/Backlog.md`](docs/Backlog.md).
- **Android 16 or newer only.** The app will not install on older versions. Only one phone model (Motorola moto g stylus 2025) was used for testing; there was no emulator or tablet testing.
- **Phase 2 and 3 are not built:** no accounts, no cloud sync, no OCR or AI, no location, no voice notes, no web clipper, no database encryption. See [`docs/Backlog.md`](docs/Backlog.md).
- **After Android stops the app in the background**, it reopens on the notes list; an unfinished draft, open dialogs, the search text and the filters are lost. Rotating the phone is handled.
- **Case-insensitive matching works only for ASCII letters** in search, tags and category names ("Ção" and "ção" count as different).
- **Links are plain text**, not clickable.
- **No undo for deleting a note or a category.** Deleted notes are only hidden in the database; there is no screen to restore them.
- **No error screen if the database cannot be opened**, and a failed database read is not caught; in both cases the app would crash.
- If a note is deleted while its edit screen is open (only possible from outside this app's screens), the app returns to the list without a message and the draft is lost.
- A category created from the note form's "New category…" dialog is kept even if the note is then cancelled.
- Rotating the phone closes open dropdown menus and removes a visible message bar.
- A note saved before the length limits existed and longer than a limit is cut at the first keystroke in Edit.
- Touch-target sizes rely on Material 3 defaults and were not checked with an accessibility scanner.

---

## 📜 Credits and academic integrity

- **Planning and code assistance:** Claude and Claude Code (Anthropic), used as described in [How this project is being built](#-how-this-project-is-being-built) and recorded sprint by sprint in [`SPRINT_LOG.md`](SPRINT_LOG.md). The application code was written by Claude Code from prompts written by Claude; the team directed the work, tested every sprint by hand and is responsible for being able to explain it.
- **Libraries and tools**, used under their own licenses (Apache License 2.0 for the Android, Kotlin, Gradle and KSP components; Eclipse Public License 1.0 for JUnit):
  - Kotlin, Kotlin coroutines and the Compose compiler plugin (JetBrains)
  - AndroidX: Jetpack Compose and Material 3, Activity, Lifecycle (ViewModel and runtime), Room, AndroidX Test and Espresso (Google)
  - Android Gradle Plugin (Google), Gradle (Gradle Inc.), KSP (Google)
  - JUnit 4
- No code was copied from tutorials or other projects, and no images, icons, fonts or other third-party assets are included; the app uses the system default launcher icon.
- No real confidential data is used in this project.

---

<div align="center">

Made for the **Database Security** course · Group A · Instructor and tutor: Professor Martin Lee

</div>
