# PKCO — Product Backlog

Last updated: October 10, 2026 · Group A

This is the living list of everything planned outside Phase 1, plus the items found while reviewing the project documents before Sprint 1. [Milestone 2.1](Milestone-2.1-Feature-and-Workflow.md) stays as the snapshot of what was agreed; this file is where status changes are recorded.

**How to use it:** change the Status when work starts or finishes, add a row for each new idea, and never reuse an ID. Phases marked "proposed" have not been confirmed by the team.

## Items found in the document review

These items were not in the original plan. They were identified, discussed and listed while reviewing Milestones 1 and 2 and the focus-group transcript, before any code was written. They should have been part of the initial consideration, so they are recorded here with their origin.

| ID | Item | Phase | Origin | Status |
| --- | --- | --- | --- | --- |
| B-05 | "Just save, or add context?" prompt after a screenshot | 2 (proposed; part of AI extraction) | Meeting transcript | Not started |
| B-06 | Encrypted local database | 2 (proposed) | Review of course context; no encryption in Phase 1 by decision of October 10, 2026 | Not started |
| B-10 | Status on items: wishlist, to do, in progress, done; read and unread | 3 (proposed) | Meeting transcript | Not started |
| B-11 | Events and places as item types | 3 (proposed) | Meeting transcript | Not started |
| B-12 | Goals with time horizons, such as 20 books this year | 3 (proposed) | Meeting transcript | Not started |
| B-13 | Certification suggestions and where to take them | 3 (proposed) | Meeting transcript | Not started |
| B-14 | Import or consolidate from Notion, Trello and similar apps | 3 (proposed) | Meeting transcript | Not started |
| B-15 | Nested folders | 3 (proposed) | Review of Milestone 2 | Not started |

## Planned in Milestone 2

| ID | Item | Phase | Origin | Status |
| --- | --- | --- | --- | --- |
| B-01 | Authentication | 2 | Milestone 2 workflow; made explicit in Milestone 2.1 | Not started |
| B-02 | Cloud sync and multi-device access | 2 | Milestone 2 | Not started |
| B-03 | AI text and image extraction (OCR, suggested categories and tags) | 2 | Milestone 2 | Not started |
| B-04 | Automatic timestamps and geolocation | 2 | Milestone 2 | Not started |
| B-07 | Voice-to-text notes | 3 | Milestone 2 | Not started |
| B-08 | Web clipper extension | 3 | Milestone 2 | Not started |
| B-09 | AI summaries and flashcards | 3 | Milestone 2 | Not started |

## Gaps resolved in Milestone 2.1

These were also found in the review, but they affect Phase 1 and were settled before coding started.

| Gap found | Resolution |
| --- | --- |
| Milestone 1 listed image-to-text in the MVP; Milestone 2 places it in Phase 2 | Milestone 2 governs; Phase 1 has no image features |
| Edit and delete flows were not described | Added to Phase 1 |
| "Tags and folders/categories" did not say how they relate | One optional category per note, plus free tags |
| "SQLite" and "Room" were both named | Room over SQLite |
| "Sorted by relevance" was not defined | Scoring rule: title 3, tag or category 2, body 1, then most recent |
| Authentication sat in Phase 3, but cloud sync needs it | Authentication is a Phase 2 feature |
| Technology stack, minimum Android version and test device were not stated | Kotlin, Jetpack Compose, Room, Android 16, physical Android 16 phone |
