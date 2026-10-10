# Personal Knowledge and Context Organizer (PKCO) — Feature & Workflow Document

> Converted to Markdown from the submitted PDF `Project Progress Report — Milestone 2.pdf`. The text is kept as submitted; only the formatting was adapted. This file is a historical record and is not edited after the fact. Changes agreed afterwards are in [Milestone 2.1](Milestone-2.1-Feature-and-Workflow.md).

## 1. Feature Breakdown by Phase

### Phase 1 – Core Features / MVP (Minimum Viable Product)

These are the essential features required for the app to serve its fundamental purpose of capturing and organizing information so users can offload memorization.

- **Unified Information Capture:** Allows users to manually create, edit, and save text-based notes, links, and study snippets directly within a clean Android interface.
- **Basic Metadata Tagging & Categorization:** Enables users to assign custom tags and folders/categories to each captured item to keep information organized.
- **Local Data Persistence (SQLite):** Stores all user notes, tags, and categories locally on the Android device using a robust relational database structure, ensuring data availability offline.
- **Basic Search & Retrieval:** Provides a text-based search bar allowing users to quickly look up saved notes and snippets by title, keyword, or tag.

### Phase 2 – Competitive and Future-Ready Features

These features distinguish the app from standard note-taking tools. While not implemented in Phase 1, the architecture will be prepared for them.

- **AI-Assisted Text & Image Extraction:** Automatically reads uploaded screenshots or reference images using computer vision/OCR and prompts the user with AI-generated categorization suggestions.
- **Cloud Synchronization & Multi-Device Access:** Syncs user notes and metadata securely to a cloud backend (e.g., Firebase or a custom REST API) so data is accessible across multiple devices.
- **Automated Contextual Metadata (Geolocation & Timestamps):** Automatically captures device timestamps and geolocation coordinates when a note or photo is saved to provide richer context.

### Phase 3 – Nice-to-Have Features

These features enhance long-term usability and convenience but are not critical for the core functionality.

- **Voice-to-Text Audio Notes:** Allows users to record voice memos and automatically transcribes them into searchable text notes.
- **Web Clipper Browser Extension:** A companion browser plugin to instantly save articles and web snippets directly into the PKCO app account.
- **Smart AI Summarization & Flashcards:** Automatically generates flashcards or summaries from complex study notes to aid lifelong learning and exam preparation.

## 2. Justifications for Phase Assignments

- **Phase 1 Justification:** Features like manual capture, local storage, basic tagging, and search are indispensable. Without them, the app has no foundational functionality to fulfill its primary goal of organizing scattered thoughts.
- **Phase 2 Justification:** Image extraction, cloud sync, and auto-metadata elevate PKCO above basic notepad apps, making it competitive. However, they depend on external APIs and backend infrastructure, which add complexity that should be tackled only after the MVP is stable.
- **Phase 3 Justification:** Voice memos, browser extensions, and flashcard generation provide high-value convenience. They represent future enhancements once the core app and cloud sync are fully matured.

## 3. Technical Preparation for Phase 2 in Phase 1 Design

To ensure the Phase 1 app can seamlessly transition into Phase 2 without requiring a total rewrite, the following architectural preparations will be established immediately:

- **Repository & Data Access Abstraction:** The Phase 1 SQLite database access will be implemented using a repository pattern (or an ORM like Room in Android). This decouples data storage logic from the user interface, ensuring that swapping local SQLite storage for cloud synchronization or adding a network layer in Phase 2 will only require updating the data repository layer.
- **Modular Data Models:** Data entities (such as Note and Tag) will be designed with clean data transfer object (DTO) patterns and unique identifiers (UUIDs) right from Phase 1. This prevents primary key conflicts when local records are eventually merged with a cloud database in Phase 2.
- **Extensible Metadata Schema:** The database schema for notes will include flexible JSON-compatible storage fields or extensible metadata columns (such as placeholders for latitude, longitude, and source_image_url) so that adding automated geolocation and image references later will not require a destructive database migration.

## 4. End-to-End User Workflow Document (Phase 1 & 2 Roadmap)

This section outlines how users will interact with the PKCO application, serving as the blueprint for our system design and future AI prompting.

**Workflow Scenario: Capturing and Organizing Study Material**

1. **App Launch & Authentication:**
    - The user opens the PKCO Android application. In Phase 1, the app opens directly to the local dashboard. (In Phase 2, a lightweight local login/sync check runs in the background).
2. **Information Capture:**
    - **Text/Link Entry:** The user taps the floating "+" action button, selects "New Note", enters a title and body text or pastes a link, and taps "Save".
    - **Future Image/Screenshot Capture (Phase 2 Preview):** The user selects "Capture Image", snaps a photo of a whiteboard or uploads a screenshot. The app's OCR engine extracts the text, and an AI prompt suggests relevant tags.
3. **Categorization & Tagging:**
    - The user assigns or selects existing custom tags (e.g., "Architecture", "Exams", "Ideas") from a dropdown menu.
    - In Phase 2, the app automatically appends timestamp and geolocation metadata to the saved record.
4. **Storage & Persistence:**
    - The application saves the structured note object into the local SQLite database via the data repository abstraction layer. (In Phase 2, this triggers a background sync payload to the cloud server).
5. **Retrieval & Review:**
    - The user navigates to the home dashboard or uses the search bar, typing a keyword.
    - The app instantly queries the local database (or cloud index in Phase 2) and displays matching notes sorted by recency and relevance, completing the information-offloading cycle.
