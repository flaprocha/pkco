# PKCO — Demo Script and Final Manual Test

This script walks through everything PKCO does in about 12 minutes. It doubles as the final manual test of Sprint 8 and as the outline of the recorded team meeting. Run it on a phone with a fresh install (see the README), so the app starts with no notes.

**Before you start:** turn on auto-rotate on the phone, and install the app with `installDebug` (not right after `connectedDebugAndroidTest`, which wipes the data).

## 1. Start (1 min)

1. Open PKCO. Expected: the "PKCO" top bar with a "Categories" button, the search field, the "No notes yet. Tap + to add one." text and the "+" button.

## 2. Categories (2 min)

1. Tap **Categories**. Expected: an empty hint.
2. Add **Study**, **Home** and **Work**. Try adding `study` again: it is rejected as a duplicate. Try an empty name: it is rejected.
3. Rename **Work** to **Job**, then back to **Work**. Go back to the list.

## 3. Create notes (3 min)

Create these four notes in this order (tap "+", fill in, Save). Choose the category from the dropdown and add tags with the Add button.

| Note | Title | Body | Category | Tags |
| --- | --- | --- | --- | --- |
| A | Kotlin basics | learn coroutines | Study | android |
| B | Shopping | buy a book | Home | kotlin |
| C | Ideas | kotlin android | none | none |
| D | Meeting | release plan | Work | study |

Expected: each card shows the title, a body preview, the category and tags, and the "Edited" time. The newest is on top. Adding the tag `KOTLIN` after `kotlin` on the same note does nothing.

## 4. Search and relevance (2 min)

| Search | Expected order |
| --- | --- |
| `kotlin` | A (title match), B (tag match), C (body match); D hidden |
| `android` | A (tag match), C (body match) |
| `kotlin android` | A first (5 points), then C, then B (2 points each; C was edited more recently) |
| `KOT` | A, B, C (partial and case-insensitive) |
| `study` | D, then A (2 points each; D was edited more recently) |
| `zzz` | "Nothing found. Try other words or clear the filters." |

Tap **Clear**: all four notes return.

## 5. Filters (1 min)

1. Category = **Study**: only A. Set Category back to **All**, then Tag = **kotlin**: only B. Set Tag back to **All**.
2. Category = **Study** and search `kotlin`: only A.
3. Open **Categories**, delete **Study**, go back: the category filter resets to "All", and notes A (and others in it) stay, now without a category.

## 6. Edit and delete (1 min)

1. Open note C, tap **Edit**, change the title and Save: the View screen shows the new title and the note moves to the top of the list.
2. Edit with no changes and Save: nothing is written, the note does not move.
3. Open a note, tap **Delete**, choose Cancel (the note stays), delete again and confirm (the note disappears).

## 7. Robustness (1 min)

1. Rotate the phone on the create form with text typed: the text stays. Rotate on the list with a search and a filter active: both stay.
2. Paste a very long text (more than 200 characters) into the title: it stops at 200 and the counter shows "200 / 200".
3. Close the app completely (swipe it away) and reopen it: the notes, categories and tags are still there.

## 8. Behind the scenes (for the recording)

- Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat connectedDebugAndroidTest` and show the passing counts (then reinstall with `installDebug`).
- Open `SPRINT_LOG.md` and show the eight sprints: times, what Claude and Claude Code got wrong or added, and what the human had to do.
- Open `docs/Backlog.md` and the Known limitations in the README to show what is deliberately not built.
