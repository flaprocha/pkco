<#
.SYNOPSIS
  PKCO release audit. Writes a Markdown report to audit\results\.

.DESCRIPTION
  Read-only. It never changes the repository, never reads the keystore or any password, and never
  prints the VALUE of anything it finds: only file names, line numbers, counts, hashes and
  public certificate fingerprints. Local folder names are replaced by <repo> in the report.

  Needs: git, a release APK and AAB already built, ANDROID_HOME (build-tools) and JAVA_HOME
  (keytool, jarsigner). Gradle is used only for the dependency check (skip it with -SkipGradle).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\audit\pkco-audit.ps1
  powershell -ExecutionPolicy Bypass -File .\audit\pkco-audit.ps1 -SkipGradle -SkipWeb
#>
param(
  [string]$Apk = 'app\build\outputs\apk\release\app-release.apk',
  [string]$Aab = 'app\build\outputs\bundle\release\app-release.aab',
  [string]$OutDir = 'audit\results',
  [switch]$SkipGradle,
  [switch]$SkipWeb
)

$ErrorActionPreference = 'Continue'
$startTime = Get-Date

# ---------- repository root ----------
$rootRaw = git rev-parse --show-toplevel 2>$null
if (-not $rootRaw) { Write-Error 'Run this script inside the PKCO git repository.'; exit 2 }
$root = (Resolve-Path $rootRaw).Path
Set-Location $root
$excludeAudit = ':(exclude)audit'

# ---------- helpers ----------
$checks = New-Object System.Collections.Generic.List[object]

function Add-Check {
  param([string]$Area, [string]$Name, [string]$Status, [string[]]$Evidence = @(), [string]$Note = '')
  $checks.Add([pscustomobject]@{
      Id       = ('C{0:00}' -f ($checks.Count + 1))
      Area     = $Area
      Name     = $Name
      Status   = $Status
      Evidence = @($Evidence | Where-Object { $_ -ne $null })
      Note     = $Note
    })
  Write-Host ('[{0}] {1}: {2}' -f $Status, $Area, $Name)
}

function Clean([string]$s) {
  if ($null -eq $s) { return '' }
  return $s.Replace($root, '<repo>').Replace($root.Replace('\', '/'), '<repo>')
}

function Short-Lines($lines) {
  # git grep -n prints path:line:content. Keep only path:line so no content (and no secret) is printed.
  @($lines | ForEach-Object { $_ -replace '^([^:]+:\d+):.*$', '$1' })
}

function Run-Tool {
  param([string]$Exe, [string[]]$Arguments)
  try { return @(& $Exe @Arguments 2>&1 | ForEach-Object { "$_" }) } catch { return @("ERROR: $($_.Exception.Message)") }
}

# ---------- tools ----------
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
$btDir = $null
if ($sdk -and (Test-Path (Join-Path $sdk 'build-tools'))) {
  $btDir = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory |
    Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } -Descending | Select-Object -First 1
}
$aapt2 = $null; $apksigner = $null
if ($btDir) {
  $aapt2 = Join-Path $btDir.FullName 'aapt2.exe'
  $apksigner = Join-Path $btDir.FullName 'apksigner.bat'
}
$jarsigner = $null; $keytool = $null; $javaExe = $null
if ($env:JAVA_HOME) {
  $jarsigner = Join-Path $env:JAVA_HOME 'bin\jarsigner.exe'
  $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
  $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
}

# ---------- metadata ----------
$commitFull = (git rev-parse HEAD)
$commit = (git rev-parse --short HEAD)
$branch = (git rev-parse --abbrev-ref HEAD)
$tags = (@(git tag --points-at HEAD) -join ', ')
$commitCount = (git rev-list --all --count)
$remote = (git remote get-url origin 2>$null)
$javaLine = ''
if ($javaExe -and (Test-Path $javaExe)) { $javaLine = (Run-Tool $javaExe @('-version') | Select-Object -First 1) }

# =====================================================================
# 1. Build artifacts
# =====================================================================
Write-Host 'Checking build artifacts...'
$haveApk = Test-Path $Apk
$haveAab = Test-Path $Aab
$apkHash = ''; $aabHash = ''

$dirtyRaw = @(git status --porcelain -- . $excludeAudit ':(exclude)audit/results' 2>$null)
if ($dirtyRaw.Count -eq 0) {
  Add-Check 'Build' 'Working tree is clean (the audited commit is what was built)' 'PASS'
} else {
  Add-Check 'Build' 'Working tree is clean (the audited commit is what was built)' 'WARN' @($dirtyRaw | ForEach-Object { $_ }) 'Uncommitted changes exist. The artifacts may not match the commit.'
}

$ct = (git log -1 --format=%ct -- app gradle build.gradle.kts settings.gradle.kts gradle.properties 2>$null)
$srcTime = $null
if ($ct) { $srcTime = [DateTimeOffset]::FromUnixTimeSeconds([int64]$ct).LocalDateTime }

foreach ($pair in @(@('APK', $Apk, $haveApk), @('AAB', $Aab, $haveAab))) {
  $label = $pair[0]; $path = $pair[1]; $exists = $pair[2]
  if (-not $exists) {
    Add-Check 'Build' "$label exists" 'FAIL' @() "Not found: $path. Build the release first."
    continue
  }
  $item = Get-Item $path
  $hash = (Get-FileHash $path -Algorithm SHA256).Hash.ToLower()
  if ($label -eq 'APK') { $apkHash = $hash } else { $aabHash = $hash }
  $ev = @("size: $($item.Length) bytes", "modified: $($item.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))", "sha256: $hash")
  if ($srcTime -and $item.LastWriteTime -lt $srcTime) {
    Add-Check 'Build' "$label is newer than the last source change" 'FAIL' $ev "Stale file: it is older than the last commit that changed app code ($($srcTime.ToString('yyyy-MM-dd HH:mm:ss'))). Rebuild with clean."
  } else {
    Add-Check 'Build' "$label is newer than the last source change" 'PASS' $ev
  }
}

# =====================================================================
# 2. Repository: secrets and sensitive files
# =====================================================================
Write-Host 'Scanning repository...'
$sensitive = @(git ls-files | Where-Object { $_ -match '\.(jks|keystore|p12|pfx|pem|key|env)$|(^|/)local\.properties$|google-services|secrets' })
if ($sensitive.Count -eq 0) { Add-Check 'Secrets' 'No keystore, key, env or local.properties file is tracked' 'PASS' }
else { Add-Check 'Secrets' 'No keystore, key, env or local.properties file is tracked' 'FAIL' $sensitive }

$notIgnored = @()
foreach ($probe in 'probe.jks', 'probe.keystore', 'local.properties') {
  git check-ignore -q -- $probe 2>$null
  if ($LASTEXITCODE -ne 0) { $notIgnored += $probe }
}
if ($notIgnored.Count -eq 0) { Add-Check 'Secrets' '.gitignore excludes *.jks, *.keystore and local.properties' 'PASS' }
else { Add-Check 'Secrets' '.gitignore excludes *.jks, *.keystore and local.properties' 'FAIL' $notIgnored 'Not ignored by .gitignore.' }

# Literal secret assignments. The pattern goes through a file so that quote characters survive Windows PowerShell.
$q = [char]34
$patternFile = Join-Path $env:TEMP ('pkco-audit-pattern-' + [guid]::NewGuid().ToString('N') + '.txt')
try {
  $pat = "(password|passwd|secret|token|api[_-]?key)[$q']?[[:space:]]*[:=][[:space:]]*[$q']([^$q']+)[$q']"
  Set-Content -Path $patternFile -Value $pat -Encoding ASCII
  $hits = @(git grep -n -I -i -E -f $patternFile -- . $excludeAudit 2>$null)
} finally { Remove-Item $patternFile -Force -ErrorAction SilentlyContinue }
if ($hits.Count -eq 0) { Add-Check 'Secrets' 'No literal password, token or API key assignment in tracked files' 'PASS' }
else { Add-Check 'Secrets' 'No literal password, token or API key assignment in tracked files' 'FAIL' (Short-Lines $hits) 'File:line only; open the lines and judge. Values are not printed.' }

$pk = @(git grep -n -I -E 'BEGIN (RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY' -- . $excludeAudit 2>$null)
if ($pk.Count -eq 0) { Add-Check 'Secrets' 'No private key block in tracked files' 'PASS' }
else { Add-Check 'Secrets' 'No private key block in tracked files' 'FAIL' (Short-Lines $pk) }

$histKey = @(git log --all --oneline '-GBEGIN (RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY' -- . $excludeAudit 2>$null)
if ($histKey.Count -eq 0) { Add-Check 'Secrets' "No private key block anywhere in history ($commitCount commits, all branches)" 'PASS' }
else { Add-Check 'Secrets' 'No private key block anywhere in history' 'FAIL' $histKey 'Commits that added or removed a private key block.' }

$histPass = @(git log --all --oneline '-G(STORE|KEY)_PASSWORD[[:space:]]*=[[:space:]]*[^[:space:]<]' -- . $excludeAudit 2>$null)
if ($histPass.Count -eq 0) { Add-Check 'Secrets' 'No signing password assignment with a value anywhere in history' 'PASS' }
else { Add-Check 'Secrets' 'No signing password assignment with a value anywhere in history' 'FAIL' $histPass 'Commits that touched such a line. Review them.' }

$histNames = @(git log --all --name-only --pretty=format: | Sort-Object -Unique | Where-Object { $_ -match '\.(jks|keystore|p12|pfx|pem)$|(^|/)local\.properties$' })
if ($histNames.Count -eq 0) { Add-Check 'Secrets' 'No keystore or local.properties file name ever committed' 'PASS' }
else { Add-Check 'Secrets' 'No keystore or local.properties file name ever committed' 'FAIL' $histNames }

# =====================================================================
# 3. Source: data collection, network, logging
# =====================================================================
$srcManifest = @(git grep -n 'uses-permission' -- app/src/main/AndroidManifest.xml 2>$null)
if ($srcManifest.Count -eq 0) { Add-Check 'Privacy' 'Source manifest requests no permission' 'PASS' }
else { Add-Check 'Privacy' 'Source manifest requests no permission' 'FAIL' (Short-Lines $srcManifest) }

$net = @(git grep -n -I -i -E 'java\.net\.|okhttp|retrofit|HttpURLConnection|HttpClient|WebView|firebase|crashlytics|analytics|admob|play-services|sentry' -- app/src app/build.gradle.kts gradle/libs.versions.toml 2>$null)
if ($net.Count -eq 0) { Add-Check 'Privacy' 'No network, analytics, ads or crash-reporting code or library in source and build files' 'PASS' }
else { Add-Check 'Privacy' 'No network, analytics, ads or crash-reporting code or library in source and build files' 'FAIL' (Short-Lines $net) }

$logs = @(git grep -n -E 'Log\.|println|Timber' -- app/src/main 2>$null)
if ($logs.Count -eq 0) { Add-Check 'Privacy' 'No logging calls in app code (Log, println, Timber)' 'PASS' }
else { Add-Check 'Privacy' 'No logging calls in app code (Log, println, Timber)' 'FAIL' (Short-Lines $logs) 'Logging can leak note text.' }

# =====================================================================
# 4. Dependencies
# =====================================================================
if ($SkipGradle) {
  Add-Check 'Privacy' 'Release dependencies contain no network, analytics or ads library' 'INFO' @() 'Skipped (-SkipGradle).'
} elseif (-not (Test-Path '.\gradlew.bat')) {
  Add-Check 'Privacy' 'Release dependencies contain no network, analytics or ads library' 'WARN' @() 'gradlew.bat not found.'
} else {
  Write-Host 'Running Gradle dependency report (about 1 minute)...'
  $dep = @(& .\gradlew.bat ':app:dependencies' '--configuration' 'releaseRuntimeClasspath' '-q' 2>&1 | ForEach-Object { "$_" })
  $mods = @($dep | Select-String -Pattern '[+\\]--- ([^\s]+)' | ForEach-Object { ($_.Matches[0].Groups[1].Value -split ':')[0..1] -join ':' } | Sort-Object -Unique)
  $bad = @($mods | Where-Object { $_ -match 'firebase|analytics|admob|play-services|okhttp|retrofit|crashlytics|sentry|billing|:ads' })
  if ($mods.Count -eq 0) {
    Add-Check 'Privacy' 'Release dependencies contain no network, analytics or ads library' 'WARN' @($dep | Select-Object -First 10) 'Could not read the dependency report.'
  } elseif ($bad.Count -eq 0) {
    Add-Check 'Privacy' 'Release dependencies contain no network, analytics or ads library' 'PASS' @("distinct modules in releaseRuntimeClasspath: $($mods.Count)")
  } else {
    Add-Check 'Privacy' 'Release dependencies contain no network, analytics or ads library' 'FAIL' $bad
  }
}

# =====================================================================
# 5. APK: identity, permissions, manifest, icon
# =====================================================================
$pkgName = ''; $versionCode = ''; $versionName = ''
if ($haveApk -and $aapt2 -and (Test-Path $aapt2)) {
  Write-Host 'Reading APK manifest...'
  $badging = Run-Tool $aapt2 @('dump', 'badging', $Apk)
  $pl = $badging | Where-Object { $_ -like 'package:*' } | Select-Object -First 1
  $m = [regex]::Match("$pl", "name='([^']+)' versionCode='([^']+)' versionName='([^']*)'")
  if ($m.Success) { $pkgName = $m.Groups[1].Value; $versionCode = $m.Groups[2].Value; $versionName = $m.Groups[3].Value }
  $minSdk = ($badging | Where-Object { $_ -like 'minSdkVersion:*' } | Select-Object -First 1)
  $targetSdk = ($badging | Where-Object { $_ -like 'targetSdkVersion:*' } | Select-Object -First 1)
  Add-Check 'APK' 'Package, version and SDK levels' 'INFO' @("package: $pkgName", "versionCode: $versionCode", "versionName: $versionName", "$minSdk", "$targetSdk")

  $iconLine = $badging | Where-Object { $_ -match "^application: .*icon='[^']+'" } | Select-Object -First 1
  if ($iconLine) { Add-Check 'APK' 'Launcher icon is packaged in the release APK' 'PASS' @("$iconLine") }
  else { Add-Check 'APK' 'Launcher icon is packaged in the release APK' 'FAIL' @() 'application has no icon: the phone would show the default Android icon.' }

  $perms = @($badging | Where-Object { $_ -match "^uses-permission(-sdk-23)?: name='" } | ForEach-Object { ([regex]::Match($_, "name='([^']+)'")).Groups[1].Value })
  $own = "$pkgName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
  $other = @($perms | Where-Object { $_ -ne $own })
  $risky = @($other | Where-Object { $_ -match 'INTERNET|NETWORK|LOCATION|CAMERA|RECORD_AUDIO|CONTACTS|STORAGE|MEDIA|NOTIFICATIONS|PHONE|SMS|ACCOUNTS' })
  $permEv = @($perms | ForEach-Object { $_ })
  if ($risky.Count -gt 0) { Add-Check 'APK' 'Requested permissions' 'FAIL' $permEv 'Network, location or personal-data permission found.' }
  elseif ($other.Count -gt 0) { Add-Check 'APK' 'Requested permissions' 'WARN' $permEv 'Unexpected permission.' }
  else { Add-Check 'APK' 'Requested permissions: none that touch user data, no INTERNET' 'PASS' $permEv 'Only the app-internal signature permission added by androidx.core.' }

  $xml = Run-Tool $aapt2 @('dump', 'xmltree', $Apk, '--file', 'AndroidManifest.xml')
  $xmlText = ($xml -join "`n")
  if ($xmlText -match 'allowBackup\([^)]*\)=false') { Add-Check 'APK' 'allowBackup is false' 'PASS' } else { Add-Check 'APK' 'allowBackup is false' 'FAIL' }
  if ($xmlText -match 'dataExtractionRules') { Add-Check 'APK' 'dataExtractionRules is set (device transfer excluded)' 'PASS' } else { Add-Check 'APK' 'dataExtractionRules is set (device transfer excluded)' 'FAIL' }
  if ($xmlText -match 'debuggable\([^)]*\)=true') { Add-Check 'APK' 'App is not debuggable' 'FAIL' } else { Add-Check 'APK' 'App is not debuggable' 'PASS' }
  if ($xmlText -match 'usesCleartextTraffic\([^)]*\)=true') { Add-Check 'APK' 'Cleartext traffic is not enabled' 'FAIL' } else { Add-Check 'APK' 'Cleartext traffic is not enabled' 'PASS' }

  $exported = @(); $el = ''; $nm = ''
  foreach ($line in $xml) {
    $mm = [regex]::Match($line, '^\s*E: (\S+)')
    if ($mm.Success) { $el = $mm.Groups[1].Value; $nm = '' ; continue }
    $mn = [regex]::Match($line, ':name\(0x[0-9a-fA-F]+\)="([^"]+)"')
    if ($mn.Success) { $nm = $mn.Groups[1].Value }
    if ($line -match 'exported\(0x[0-9a-fA-F]+\)=true') { $exported += "$el $nm" }
  }
  $ownExported = @($exported | Where-Object { $_ -match 'com\.example\.pkco' -and $_ -notmatch 'MainActivity' })
  if ($ownExported.Count -gt 0) { Add-Check 'APK' 'Exported components' 'FAIL' $exported 'An app component other than the launcher activity is exported.' }
  else { Add-Check 'APK' 'Exported components: only the launcher activity (plus library receivers)' 'PASS' $exported }
} else {
  Add-Check 'APK' 'APK manifest checks' 'WARN' @() 'Skipped: APK or aapt2 not available (set ANDROID_HOME and build the release).'
}

# =====================================================================
# 6. Signatures
# =====================================================================
$apkCert = ''
if ($haveApk -and $apksigner -and (Test-Path $apksigner)) {
  $sig = @(& $apksigner verify --verbose --print-certs $Apk 2>$null | ForEach-Object { "$_" })
  $v2 = $sig | Where-Object { $_ -match 'v2 scheme.*: true' }
  $cm = [regex]::Match(($sig -join "`n"), 'certificate SHA-256 digest: ([0-9a-fA-F]{64})')
  if ($cm.Success) { $apkCert = $cm.Groups[1].Value.ToLower() }
  $isDebug = ($sig -join "`n") -match 'Android Debug'
  if ($v2 -and $apkCert -and -not $isDebug) { Add-Check 'Signing' 'APK is signed (v2) with a non-debug certificate' 'PASS' @("certificate SHA-256: $apkCert") 'The fingerprint is public. Compare it with the upload certificate registered in Play Console.' }
  else { Add-Check 'Signing' 'APK is signed (v2) with a non-debug certificate' 'FAIL' @($sig | Select-Object -First 6) }
} else {
  Add-Check 'Signing' 'APK signature' 'WARN' @() 'Skipped: APK or apksigner not available.'
}

if ($haveAab -and $jarsigner -and (Test-Path $jarsigner)) {
  $js = Run-Tool $jarsigner @('-verify', $Aab)
  if (($js | Select-Object -First 3) -match 'jar verified') {
    Add-Check 'Signing' 'AAB signature verifies' 'PASS' @('jar verified') 'jarsigner warnings (self-signed, no timestamp, JarInputStream notes) are normal for an upload key and an AAB.'
  } else { Add-Check 'Signing' 'AAB signature verifies' 'FAIL' @($js | Select-Object -First 5) }
  if ($keytool -and (Test-Path $keytool) -and $apkCert) {
    $kt = (Run-Tool $keytool @('-printcert', '-jarfile', $Aab)) -join "`n"
    $km = [regex]::Match($kt, '([0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){31})')
    $aabCert = ''
    if ($km.Success) { $aabCert = ($km.Groups[1].Value -replace ':', '').ToLower() }
    if ($aabCert -and $aabCert -eq $apkCert) { Add-Check 'Signing' 'AAB and APK use the same certificate (the upload key)' 'PASS' @("certificate SHA-256: $aabCert") }
    else { Add-Check 'Signing' 'AAB and APK use the same certificate (the upload key)' 'WARN' @("apk: $apkCert", "aab: $aabCert") 'Could not confirm.' }
  }
} else {
  Add-Check 'Signing' 'AAB signature' 'WARN' @() 'Skipped: AAB or jarsigner not available (set JAVA_HOME).'
}

# =====================================================================
# 7. Content of the APK and AAB
# =====================================================================
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Scan-Zip {
  param([string]$Path, [string[]]$Terms, [string]$Regex)
  $result = @{}
  foreach ($t in $Terms) { $result[$t] = New-Object System.Collections.Generic.List[string] }
  $result['__regex'] = New-Object System.Collections.Generic.List[string]
  $latin1 = [Text.Encoding]::GetEncoding(28591)
  $zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $Path).Path)
  try {
    foreach ($e in $zip.Entries) {
      if ($e.Length -eq 0 -or $e.FullName.EndsWith('/')) { continue }
      $sr = New-Object IO.StreamReader($e.Open(), $latin1)
      try { $txt = $sr.ReadToEnd() } finally { $sr.Dispose() }
      foreach ($t in $Terms) {
        $u16 = ($t.ToCharArray() -join [string][char]0)
        if ($txt.IndexOf($t, [StringComparison]::Ordinal) -ge 0 -or $txt.IndexOf($u16, [StringComparison]::Ordinal) -ge 0) { $result[$t].Add($e.FullName) }
      }
      if ($Regex -and [regex]::IsMatch($txt, $Regex)) { $result['__regex'].Add($e.FullName) }
    }
  } finally { $zip.Dispose() }
  return $result
}

$criticalTerms = @('PRIVATE KEY', 'storePassword', 'keyPassword')
$infoTerms = @('.jks', '.keystore', 'android.permission.INTERNET')
$pathRegex = '[A-Za-z]:\\(Users|Tools|Documents|Projects)\\'
foreach ($pair in @(@('APK', $Apk, $haveApk), @('AAB', $Aab, $haveAab))) {
  $label = $pair[0]; $path = $pair[1]; $exists = $pair[2]
  if (-not $exists) { continue }
  Write-Host "Scanning $label content..."
  try {
    $r = Scan-Zip -Path $path -Terms ($criticalTerms + $infoTerms) -Regex $pathRegex
    $crit = @(); foreach ($t in $criticalTerms) { foreach ($f in $r[$t]) { $crit += "$t in $f" } }
    if ($crit.Count -eq 0) { Add-Check 'Content' "$label contains no private key, signing password or key-store setting" 'PASS' }
    else { Add-Check 'Content' "$label contains no private key, signing password or key-store setting" 'FAIL' $crit }
    if ($r['__regex'].Count -eq 0) { Add-Check 'Content' "$label contains no local Windows folder path" 'PASS' }
    else { Add-Check 'Content' "$label contains no local Windows folder path" 'WARN' @($r['__regex']) 'Files that contain a drive-letter path. Path text is not printed.' }
    $inf = @(); foreach ($t in $infoTerms) { foreach ($f in $r[$t]) { $inf += "$t in $f" } }
    Add-Check 'Content' "$label mentions of .jks, .keystore or android.permission.INTERNET (information)" 'INFO' $inf 'Library code can contain these as plain text. The real permission list is in the APK check above.'
  } catch {
    Add-Check 'Content' "$label content scan" 'WARN' @() "Could not run: $($_.Exception.Message)"
  }
}

# =====================================================================
# 8. Privacy policy
# =====================================================================
$policyFile = Test-Path 'docs\index.md'
if ($policyFile) { Add-Check 'Policy' 'Privacy policy source exists (docs/index.md)' 'PASS' } else { Add-Check 'Policy' 'Privacy policy source exists (docs/index.md)' 'FAIL' }
if ($SkipWeb) {
  Add-Check 'Policy' 'Privacy policy page is online' 'INFO' @() 'Skipped (-SkipWeb).'
} else {
  $wm = [regex]::Match("$remote", 'github\.com[:/]([^/]+)/([^/]+?)(\.git)?$')
  if ($wm.Success) {
    $url = 'https://{0}.github.io/{1}/' -f $wm.Groups[1].Value.ToLower(), $wm.Groups[2].Value
    try {
      [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
      $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 20
      if ($resp.StatusCode -eq 200 -and $resp.Content -match '(?i)privacy') { Add-Check 'Policy' 'Privacy policy page is online' 'PASS' @("$url -> HTTP $($resp.StatusCode)") }
      else { Add-Check 'Policy' 'Privacy policy page is online' 'WARN' @("$url -> HTTP $($resp.StatusCode)") 'The page did not look like a privacy policy.' }
    } catch { Add-Check 'Policy' 'Privacy policy page is online' 'FAIL' @($url) "Request failed: $($_.Exception.Message)" }
  } else {
    Add-Check 'Policy' 'Privacy policy page is online' 'WARN' @() 'Could not derive the GitHub Pages address from the origin remote.'
  }
}

# =====================================================================
# Report
# =====================================================================
$fails = @($checks | Where-Object { $_.Status -eq 'FAIL' })
$warns = @($checks | Where-Object { $_.Status -eq 'WARN' })
$passes = @($checks | Where-Object { $_.Status -eq 'PASS' })
$infos = @($checks | Where-Object { $_.Status -eq 'INFO' })

$sb = New-Object System.Text.StringBuilder
function W([string]$s = '') { [void]$sb.AppendLine($s) }
$fence = '```'

W '# PKCO release audit'
W ''
W ("Generated: {0}  " -f $startTime.ToString('yyyy-MM-dd HH:mm:ss'))
W ('Commit: `{0}`{1} on `{2}` ({3} commits in history)  ' -f $commit, $(if ($tags) { " (tag $tags)" } else { '' }), $branch, $commitCount)
W 'Script: `audit/pkco-audit.ps1` (read-only; prints no secret values)'
W ''
W '## Result'
W ''
if ($fails.Count -eq 0) {
  W ('**No blocking finding.** {0} checks passed, {1} warnings, {2} information items.' -f $passes.Count, $warns.Count, $infos.Count)
} else {
  W ('**{0} blocking finding(s).** {1} checks passed, {2} warnings.' -f $fails.Count, $passes.Count, $warns.Count)
}
W ''
W '| Item | Value |'
W '| --- | --- |'
W ('| Application ID | {0} |' -f $pkgName)
W ('| versionCode / versionName | {0} / {1} |' -f $versionCode, $versionName)
W ('| APK SHA-256 | {0} |' -f $apkHash)
W ('| AAB SHA-256 | {0} |' -f $aabHash)
W ('| Signing certificate SHA-256 (public) | {0} |' -f $apkCert)
W ('| Build tools | {0} |' -f $(if ($btDir) { $btDir.Name } else { 'not found' }))
W ('| Java | {0} |' -f (Clean $javaLine))
W ''
W '## Checks'
W ''
W '| ID | Area | Check | Result |'
W '| --- | --- | --- | --- |'
foreach ($c in $checks) { W ('| {0} | {1} | {2} | {3} |' -f $c.Id, $c.Area, $c.Name, $c.Status) }
W ''
W '## Evidence'
W ''
W 'Only file names, line numbers, counts, hashes and public fingerprints are shown. Local folders appear as `<repo>`.'
W ''
foreach ($c in $checks) {
  W ('### {0} {1}: {2} [{3}]' -f $c.Id, $c.Area, $c.Name, $c.Status)
  if ($c.Note) { W ''; W $c.Note }
  if ($c.Evidence.Count -gt 0) {
    W ''
    W $fence
    foreach ($l in ($c.Evidence | Select-Object -First 40)) { W (Clean $l) }
    if ($c.Evidence.Count -gt 40) { W ('... {0} more lines' -f ($c.Evidence.Count - 40)) }
    W $fence
  }
  W ''
}
W '## What this script does not prove'
W ''
W '- It does not read the keystore or any password, so it cannot tell whether the keystore itself is safe; keep it outside the repository and back it up.'
W '- It checks the code, the build files and the packaged files. Runtime behaviour (for example what reaches the system log) needs a manual test on a device.'
W '- Google Play re-signs the app with its own key when Play App Signing is on; the fingerprint above is the upload key.'
W ''
W ('Run time: {0:N0} seconds.' -f ((Get-Date) - $startTime).TotalSeconds)

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force $OutDir | Out-Null }
$suffix = ''
if ($dirtyRaw.Count -gt 0) { $suffix = '-dirty' }
$outFile = Join-Path $OutDir ('audit-{0}-{1}{2}.md' -f $startTime.ToString('yyyyMMdd-HHmm'), $commit, $suffix)
[IO.File]::WriteAllText((Join-Path $root $outFile), $sb.ToString(), (New-Object Text.UTF8Encoding($false)))

Write-Host ''
Write-Host ('PASS {0}  WARN {1}  FAIL {2}  INFO {3}' -f $passes.Count, $warns.Count, $fails.Count, $infos.Count)
Write-Host "Report: $outFile"
if ($fails.Count -gt 0) { exit 1 } else { exit 0 }
