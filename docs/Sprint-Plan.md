# PKCO — Sprint Plan (Sprints 1–8)

October 10, 2026 · Group A (Sabrina Martins, Felipe Rocha)

## How the sprints run

Eight sprints follow the Instruction3 cycle, and Felipe runs every step. Milestone 2.1 is the scope baseline: nothing outside its Phase 1 is built.

1. Discuss the next task with Claude.
2. Claude writes the prompt for Claude Code.
3. Felipe gives the prompt to Claude Code, which does the work.
4. Felipe reviews Claude Code's report and the app on the phone.
5. Felipe brings the result back to Claude for analysis and the next step.
6. Felipe records decisions, deviations and surprises in the sprint log.

Every Claude Code prompt contains the sprint goal, the areas it may change, a "do not" list (no Phase 2 or 3 features, no new libraries unless named), acceptance criteria, and a request for a short report: what changed, what was skipped, deviations from the prompt, tests run, and the commit. Felipe follows Claude's recommendations as given; every time he has to intervene, it goes in the log.

All eight sprints are being compressed into one or two days because of the October 10 deadline (an extension has been requested). Each sprint log entry records start and end times, and the final report states this limit.

## Sprint 1 setup checklist

Setup is the first part of Sprint 1: it makes sure the phone is reachable and Claude Code works before any app code is written, so Sprint 1 covers both setup and the project skeleton.

- [ ] Claude Pro is active and you are signed in
- [ ] Install Android Studio on Windows 11 with its default setup (it brings the Android SDK and its own JDK)
- [ ] In Android Studio, set the Gradle JDK to the bundled one, so builds do not depend on the JDK 25.0.1 already on the computer
- [ ] On the phone, tap Build number seven times in the About phone screen, then switch on USB debugging in Developer options (menu wording varies by maker)
- [ ] Connect the phone with a data cable and accept the USB debugging prompt; the phone should appear in Android Studio's device list
- [ ] Install Git for Windows and clone `https://github.com/flaprocha/pkco-android`
- [ ] Install Claude Code by following Anthropic's official setup instructions, sign in with the Pro account, and open it in the cloned folder
- [ ] Save Milestone 2.1 and this document as Markdown in the `docs` folder of the repo
- [ ] Create an empty `SPRINT_LOG.md` in the repo using the template at the end of this document

## Roadmap: Sprints 1 to 8

The eight sprints build Phase 1 in layers, from an empty app to a documented, demonstrable one; each sprint ends with a working app on the phone.

| Sprint | Goal | Done when |
| --- | --- | --- |
| 1 | Setup and project skeleton | The setup checklist is complete, and a Kotlin and Compose project (minimum Android 16, Room added) runs on the phone and shows an empty notes list; a project guide file for Claude Code is written from Milestone 2.1; committed |
| 2 | Data layer | Room tables for notes, categories, tags and their link, the repository interface and implementation, exported schema, and unit tests passing |
| 3 | List and create | A note can be created with title, body and link, saved to Room, and appears in the list after restarting the app |
| 4 | Edit and delete | A note can be opened, edited and saved, and deleted after confirmation |
| 5 | Categories and tags | Categories can be created, renamed, deleted and assigned; tags can be added to and removed from notes |
| 6 | Search | Search returns results in the agreed relevance order, the list can be filtered by category or tag, and scoring has unit tests |
| 7 | Quality | Empty and error states, input checks, screen rotation handled, a basic UI test, and known bugs fixed or listed |
| 8 | Documentation and demo | README (install, technologies, features, limitations, how to test), AI and outside code credits, final test pass, and a demo script |

## Observations before coding

Claude's own behavior before any code is already worth logging: five small additions or errors of mine, three useful catches and one tooling problem, listed with the Instruction3 category each falls under.

| Observation | Category | Effect |
| --- | --- | --- |
| Claude said it could not read the milestone PDFs; they were readable through the project and were read after checking | Contradicts or misstates | None; corrected within one turn |
| Claude added a pre-sprint setup step that Instruction3 does not ask for | Introduces work not planned | Setup was needed anyway, but it was extra scope from Claude; Felipe merged it into Sprint 1 on October 11, keeping the plan at eight sprints |
| Claude proposed relevance weights (title 3, tag or category 2, body 1) | Adds detail not in Milestone 2 | Confirmed by Felipe on October 10 |
| Claude added list filtering by category or tag to Phase 1 | Adds scope beyond Milestone 2's wording | Flagged by a comment in Milestone 2.1 for Felipe to keep or remove |
| Claude added reserved `deletedAt` and `ownerId` columns for Phase 2 | Adds complexity | Small, but they are Claude's choice, not Milestone 2's; Felipe kept them as sync preparation |
| Claude flagged that the free plan lacks Claude Code | Useful suggestion | The Pro upgrade is the first step of the Sprint 1 setup |
| Claude noticed Milestone 1 and 2 disagree on image-to-text | Useful suggestion | Milestone 2 chosen as baseline |
| Claude flagged that authentication in Phase 3 conflicts with Phase 2 cloud sync, and that Android 16 limits supported phones | Useful suggestion | Authentication moved to Phase 2; Android 16 kept because the test phone runs it |
| Attaching the GitHub repo failed twice (account not linked, then a not-found error) before a lookup found the exact name | Requires intervention | Felipe had to link GitHub; repo is now attached |

## Observations during Sprints 1 to 6

Recorded by Claude on October 12, 2026 after reviewing the Claude Code output. The sprint-by-sprint record itself is in `SPRINT_LOG.md`.

| Observation | Category | Effect |
| --- | --- | --- |
| Claude's first install steps left out the PATH fix, so `claude` was not recognized | Contradicts or misstates | Felipe fixed the user PATH; corrected within the setup |
| Claude assumed the default Android SDK path; the SDK is on a non-default drive (D:) | Contradicts or misstates | Added an environment note to the prompt |
| Claude worried that JDK 25 might not work with the build; it did | Adds unneeded caution | None |
| Claude Code stopped and asked when the C: drive was full | Useful surprise | Felipe chose D: for the session; the Gradle cache now lives on D: permanently |
| Claude Code downgraded the Compose BOM and Lifecycle to keep compileSdk 36 | Deviates from the plan | Accepted; matches the Android 16 test phone |
| Claude Code added `allowBackup="false"` and a placeholder unit test | Introduces work not planned | Harmless; the test is replaced in Sprint 2 |
| Claude Code's logged times (20:44-21:50) differ from Felipe's (20:47-21:46) | Contradicts or misstates | Felipe kept the logged times |
| The "+" button has no action | Expected | Wired up in Sprint 3 |
| Claude Code chose Room 2.8.5; Room 3 (`androidx.room3`) also exists | Decision | Felipe confirmed Room 2.8.5 and compileSdk 36 |
| Claude Code's Sprint 2 build crashed (Gradle out of native memory) because C: was full and the Windows swap file could not grow; Claude Code stopped and reported | Requires intervention; useful surprise | Felipe freed space and closed other programs; Claude had fixed the Gradle cache location but missed the swap file |
| Claude's prompt asked for "unit tests"; Room needs a device, so the tests are instrumented tests | Deviates from the plan's wording | Flagged beforehand; no new library added |
| Claude Code added behavior the prompt did not specify (category names case-insensitive, `getById`, errors on updating a missing note, and similar) | Adds detail not in Milestone 2.1 | Reasonable and reported; listed in `SPRINT_LOG.md` |
| Sprint 3: Claude Code opened the create screen with an adb tap and tested Back, which the prompt did not ask for | Useful surprise | Safe (no note was saved) and it narrowed what Felipe had to test by hand |
| Sprint 3: a failed save is not caught, so the app would crash without a message | Missing behavior | Accepted for now; error states are planned for Sprint 7 |
| Sprint 4: `connectedDebugAndroidTest` uninstalls the app and erases its data (Android's default), wiping the notes on the phone | Unexpected tooling behavior | Claude Code reinstalled and reported it; Claude had not warned about it in the prompt; the Sprint 5 prompt orders the tasks (tests first, then install) and adds a line to `CLAUDE.md` |
| Sprint 4: the start-time placeholder at the end of Claude's prompt was not replaced, so the log got a wrong date and "start time not given" | Requires intervention (prompt design) | Felipe corrected the row by hand; later prompts leave start and end times to Felipe instead of passing them through the prompt |
| Sprint 4: Claude Code renamed `CreateNoteScreen.kt` to `NoteFormScreen.kt` and added one repository test (edit keeps category and tags) | Adds detail not requested | Reasonable and reported; the test was allowed as optional |
| Sprint 4: Claude Code could not open its screenshot (outside its working folders) and used an adb UI dump instead | Tool limitation | None; Felipe's manual test covered the visual check |
| Sprint 5: Claude Code's first adb check script was broken by Git Bash path conversion and pressed Back twice, leaving the app | Own slip, reported | It relaunched the app and redid the check; no data was changed |
| Sprint 5: Claude Code made UI decisions the prompt left open (a category created from the form stays even if the note is cancelled; text left in the tag field at Save is not added; a leading `#` is kept in a tag name) | Adds detail not in Milestone 2.1 | Reported in the log; accepted |
| After Sprint 5 Felipe found the sprint times inconsistent (some from Claude Code, some from messages) | Process problem | New rule: Claude Code reads the PC clock for Start and Code finished, Felipe adds End; all times `HH:MM:SS`; rows 1 to 5 normalized |
| Sprint 6: one unit test failed first because its test input was wrong; Claude Code fixed the test, not the code | Useful behavior | Correct fix, reported |
| Sprint 6: Claude Code changed the search field's padding after the tests had passed and did not rerun them | Process slip, reported | Layout-only change; Felipe's manual test covered it |
| Sprint 6: Claude estimated 8 minutes for the manual test; it took 13:34 | Estimate too low | Expected durations are now larger |
| Sprint 6: Felipe amended a commit that was already pushed, causing a rebase conflict | Human slip | Resolved with a follow-up commit; rule: amend only before the first push |

## Sprint log template

After each sprint, fill one row, and use the deviations column for the Instruction3 observations listed below the table; copy this table into `SPRINT_LOG.md` in the repo.

**Time format (from Sprint 6 on, October 14, 2026).** Every time is `HH:MM:SS` (24-hour, two digits each) on the Eastern Time clock of Felipe's computer. The time cell has the same lines in every row:

```text
<date><br>Start: HH:MM:SS<br>Code finished: HH:MM:SS<br>Automated tests: <duration><br>Manual test (expected): ~N min<br>End: HH:MM:SS
```

Claude Code reads the computer clock itself for **Start** (its first command) and **Code finished** (just before the commit) and copies the duration Gradle prints for the automated tests. Claude gives the expected manual-test duration in the prompt. Felipe adds **End** when the manual test is done, taken from the computer clock. For Sprints 1 to 5 the seconds were not recorded; they are shown as `:00` where the clock was not read.

| Sprint | Start and end time | Result against "done when" | Deviations and unexpected AI behavior | Human intervention needed |
| --- | --- | --- | --- | --- |
| 1 |  |  |  |  |
| 2 |  |  |  |  |
| 3 |  |  |  |  |
| 4 |  |  |  |  |
| 5 |  |  |  |  |
| 6 |  |  |  |  |
| 7 |  |  |  |  |
| 8 |  |  |  |  |

In the deviations column, note any time Claude or Claude Code moved away from the plan, added features or complexity, changed the architecture, overreacted to a small problem, contradicted an earlier recommendation, or made a useful suggestion the team had not considered.
