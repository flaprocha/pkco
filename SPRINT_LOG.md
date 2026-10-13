# PKCO — Sprint Log

| Sprint | Start and end time                                        | Result against "done when" | Deviations and unexpected AI behavior | Human intervention needed |
| --- |-----------------------------------------------------------| --- | --- | --- |
| 1 | October 12, 2026, 20:44–21:50 (EDT, Claude Code session) Setup before the sprint: 17:39–20:47 | Environment checked (Android Studio, SDK with android-36, Git, phone in adb). Kotlin and Compose project with minSdk 36 and Room added builds with `assembleDebug`; 1 placeholder unit test passes; app installed on the moto g stylus 2025 and launched (screen to be confirmed on the phone). CLAUDE.md written from Milestone 2.1. Committed. | Deviations (Claude): My install steps omitted the PATH fix, so claude was not recognized. I assumed the default SDK path, but the SDK is at D:\Tools\Android\Sdk. My worry about JDK 25 did not materialize.
Deviations (Claude Code): It downgraded BOM and Lifecycle to keep compileSdk 36. It added allowBackup="false" and a placeholder test, neither of which was requested. Its end time disagreed with the real one.
Useful surprise: It stopped and asked when C: ran out of space instead of failing silently.
Human interventions: PATH fix, SDK components install, choosing D: for the session, unlocking the phone and checking the screen. |  |
| 2 |                                                           |  |  |  |
| 3 |                                                           |  |  |  |
| 4 |                                                           |  |  |  |
| 5 |                                                           |  |  |  |
| 6 |                                                           |  |  |  |
| 7 |                                                           |  |  |  |
| 8 |                                                           |  |  |  |
