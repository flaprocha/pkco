<div align="center">

# 🧠 PKCO — Personal Knowledge and Context Organizer

**One place to capture, organize and find your notes, links and study material.**

![Platform](https://img.shields.io/badge/platform-Android%2016%2B-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Storage](https://img.shields.io/badge/storage-Room%20%2F%20SQLite-lightgrey)
![Status](https://img.shields.io/badge/status-planning%20complete-orange)

</div>

---

## 📚 About this project

PKCO is an Android app built to fulfil the **Group A assignment** of the **Database Security** course, led by **Professor Martin Lee**, who is both the course instructor and our tutor and who provides the milestones and releases for the project.

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
| **1 · MVP (built here)** | Create, edit and delete text notes with links · one optional category per note · free tags · local storage with Room (works offline) · search ordered by relevance · filter by category or tag |
| **2 · Competitive** *(designed for, not built)* | Authentication · cloud sync across devices · OCR and AI extraction from screenshots · automatic timestamps and geolocation |
| **3 · Nice to have** *(ideas only)* | Voice-to-text notes · web clipper · AI summaries and flashcards · item status, events, goals and other backlog ideas |

Full details are in [`doc/Milestone-2.1-Feature-and-Workflow.md`](doc/Milestone-2.1-Feature-and-Workflow.md).

---

## 🛠️ Technology

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| Local database | Room over SQLite, behind a repository interface |
| Minimum Android version | Android 16 (API 36) |
| Development | Android Studio on Windows 11, tested on a physical Android 16 phone |
| Source control | Git and GitHub |

Outside libraries and tools are credited in the [Credits](#-credits-and-academic-integrity) section as they are added.

---

## 🗂️ Repository layout

```text
pkco-android/
├── doc/                 Project documentation
│   ├── Milestone-2.1-Feature-and-Workflow.md
│   └── Sprint-0-Setup-and-Plan.md
├── README.md            You are here
└── (Android project is added in Sprint 1)
```

---

## 🤖 How this project is being built

Part of this assignment is an experiment in AI-assisted development. Over eight short sprints, **Claude** plans each step and writes the instructions, and **Claude Code** implements them, while the team member follows the recommendations and logs where the AI helped, drifted or needed correction.

- Plan and sprint roadmap: [`doc/Sprint-0-Setup-and-Plan.md`](doc/Sprint-0-Setup-and-Plan.md)
- Sprint-by-sprint record: `SPRINT_LOG.md` (created during Sprint 0)

AI tools are permitted in this course, and the team is responsible for being able to explain all submitted code and design decisions.

---

## 🚀 Getting started

> 🚧 Installation and run instructions will be completed as the app takes shape and finalized in Sprint 8.

Planned steps:

1. Install Android Studio and open the project.
2. Connect an Android 16 phone with USB debugging on (or use an Android 16 emulator).
3. Run the `app` configuration.

## ✅ Testing

> 🚧 To be completed. The plan is unit tests for the repository and the search scoring rule, a basic UI test, and manual testing on a physical phone, with known bugs listed below.

## ⚠️ Known limitations

> 🚧 To be completed in Sprint 8. Already known: the app targets Android 16 and will not install on older versions, and Phase 2 and 3 features are not implemented.

---

## 📜 Credits and academic integrity

- Planning and code assistance: Claude and Claude Code (Anthropic), used as described above.
- Outside code, libraries, templates and tutorials will be listed here as they are used.
- No copyrighted images, logos or proprietary code are used without a license.
- No real confidential data is used in this project.

---

<div align="center">

Made for the **Database Security** course · Group A · Instructor and tutor: Professor Martin Lee

</div>
