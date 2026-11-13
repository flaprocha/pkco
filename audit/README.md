# PKCO audit

This folder keeps the release audit: the script and the reports it produced.

| Path | What it is |
| --- | --- |
| `pkco-audit.ps1` | Read-only audit script (Windows PowerShell 5.1 or PowerShell 7). |
| `results/audit-<date>-<commit>.md` | One report per run, named with the date and the audited commit. |

## What the script checks

1. **Build artifacts**: the release APK and AAB exist, are newer than the last change to the app code (catches stale files), and the working tree is clean. SHA-256 of both files.
2. **Secrets and keys**: no keystore, key, `.env` or `local.properties` file is tracked or was ever committed; `.gitignore` excludes them; no private key block and no literal password, token or API key assignment in the code or in any commit.
3. **Privacy**: the source manifest requests no permission; no network, analytics, ads or crash-reporting code or library (source, build files and the release dependency list); no logging calls in app code.
4. **APK**: package name, version, SDK levels, launcher icon present, requested permissions (no `INTERNET`), `allowBackup=false`, device-transfer rules set, not debuggable, no cleartext traffic, exported components.
5. **Signing**: APK signature v2 with a non-debug certificate; AAB signature verifies; both carry the same certificate. Only the public SHA-256 fingerprint is shown.
6. **Packaged content**: the text of every file inside the APK and AAB is searched for private key markers, signing-password names and local Windows folder paths.
7. **Privacy policy**: `docs/index.md` exists and the GitHub Pages address is online.

## What it never does

- It does not change the repository and does not read the keystore or any password.
- It never prints the value of anything it finds: only file names, line numbers, counts, hashes and public fingerprints.
- Local folders are replaced by `<repo>` in the report.

## How to run

Build the release first (see `SPRINT_LOG.md` and `CLAUDE.md`), then from the repository root:

```powershell
$env:JAVA_HOME = '<Android Studio folder>\jbr'     # session only
$env:ANDROID_HOME = '<Android SDK folder>'          # session only
powershell -ExecutionPolicy Bypass -File .\audit\pkco-audit.ps1
```

Options: `-SkipGradle` (no dependency report, saves about a minute), `-SkipWeb` (no internet check of the policy page), `-Apk` and `-Aab` (other file locations).
The exit code is `1` if any check fails, `0` otherwise.

Read the report, then commit it:

```powershell
git add audit
git commit -m "Add release audit results"
```

## Limits

- It cannot judge the keystore itself: keep it outside the repository and keep a second copy.
- Runtime behaviour (for example what reaches the system log) needs a manual test on a device.
- Google Play re-signs the app when Play App Signing is on; the fingerprint in the report is the upload key.
