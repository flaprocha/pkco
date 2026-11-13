# PKCO Final Security Audit, Release 0.1.0

November 12, 2026

The release candidate passed all 30 automated checks with no blocking finding: no private key or password in the code, the history or the packages, no INTERNET permission, and no user data collected.

Prepared jointly by Sabrina Martins and Felipe Rocha (Group A), who followed and tested every step of this release together.

## Scope and method

The audit covers the signed release files that would be uploaded to Google Play, the source code, and the whole Git history (44 commits, all branches).

A read-only PowerShell script, [`audit/pkco-audit.ps1`](https://github.com/flaprocha/pkco/tree/main/audit), ran 33 checks on November 12, 2026 against commit `c2923db` (app code last changed in Sprint 13). The script never reads the keystore or any password and prints no secret values. Its report is stored in the repository as `audit/results/audit-20261112-2103-c2923db.md`.

The checks cover five areas:

- **Secrets:** key files, key blocks and literal passwords in the tree and in every commit.
- **Privacy:** permissions, network, analytics and ads code and libraries, logging.
- **Release APK:** manifest, backup rules, debuggable flag, exported components, launcher icon.
- **Signing:** APK and AAB signatures and that both use the same certificate.
- **Packaged content:** the text inside the APK and AAB is searched for key markers and local paths.

## Answers to the three questions

Result: 30 checks passed, 0 warnings, 0 failures; 3 items are information only.

| Question | Answer | Evidence |
| --- | --- | --- |
| Is unnecessary customer information collected? | No | The release APK requests no permission that touches user data and no INTERNET permission. No network, analytics, ads or crash-reporting code or library exists in the source, the build files or the 116-module release dependency list. No logging calls in app code. Notes stay in a local database; backup and device transfer are switched off. |
| Is any private key or password hard-coded? | No | No key file is tracked or was ever committed; `.gitignore` excludes `*.jks` and `*.keystore`; no private key block and no literal password, token or API key assignment in the tree or in any commit; none inside the APK or the AAB. The signing passwords are read from Gradle properties outside the repository. |
| Is it ready to publish? | Yes, as a release candidate | The release is signed with the upload key; APK and AAB carry the same certificate; the app is not debuggable and has no cleartext traffic; the privacy policy page is online. |

The three information items are plain-text mentions of `.jks`, `.keystore` or `android.permission.INTERNET` inside library code. The real permission list was checked separately and has no INTERNET entry.

## Release identity

| Item | Value |
| --- | --- |
| Application ID | `io.github.flaprocha.pkco` |
| Version | versionCode 1, versionName 0.1.0 |
| Android | minSdk 36, targetSdk 36 (Android 16) |
| AAB built | November 12, 2026, 19:42 |
| AAB SHA-256 | `4425895191CFA31B72B8EDE6F0492822ADA7895EBD85D6887D03A861A8E7053C` |
| Upload certificate SHA-256 (public) | `2655c990e97568fd3bfafc9cbef7ff78c4880dc3857c6e26e709133d2d2f8656` |

The certificate fingerprint identifies the upload key; Google Play re-signs the app with its own key when Play App Signing is on.

## Manual checks on the phone

The script cannot see runtime behaviour, so these were done by hand on a Motorola moto g stylus 2025 (Android 16) with the signed release APK, installed from scratch on November 12, 2026:

- A note was created and the system log was searched for its title and text: no match, so note content does not reach the log.
- `run-as` on the release package answers "package not debuggable".
- The green launcher icon shows on the home screen; create, search, filter, edit, delete, rotation and reopening all worked.

Earlier sprints fixed the issues found in the first ChatGPT audit: permanent delete with purge of old rows (Sprint 9), no cloud backup or device transfer, no preview of notes in the recents screen, a production application ID and release signing (Sprint 10), and a read-error screen with a hosted privacy policy (Sprint 11). Sprint 13 fixed the landscape layout under the navigation bar. The full record is in `SPRINT_LOG.md`.

## Limits and next steps

- The script does not judge the keystore itself. It stays outside the repository; a second copy is advised, because losing it blocks future updates.
- Case-insensitive search and name matching cover ASCII letters only, and a few interface limits are listed under Known issues in `CLAUDE.md`. None affects privacy.
- Insets were tested by hand with 3-button navigation; gesture navigation and tablets were not.
- Google's first review of a new account usually takes more than 7 days; a personal account also needs a closed test with at least 12 testers enrolled for 14 days before production.

Next: create the Play Console account, set up the closed test, and report Google's result to Prof. Lee.
