<#
.SYNOPSIS
  Install Herdr App on a phone and connect it to herdr-bridge over USB (adb reverse).

.DESCRIPTION
  No Tailscale needed: the phone reaches the bridge at 127.0.0.1 through the adb connection,
  so nothing is exposed on the network. Steps:
    1. Waits for a physical device (USB, or wireless debugging after `adb pair`/`adb connect`).
    2. Optionally builds, then installs the debug APK.
    3. Sets up `adb reverse tcp:<port> tcp:<port>` and launches the app.
    4. Runs herdr-bridge in this window bound to 127.0.0.1 and prints the pairing QR.
  In the app, tap Hosts -> Scan pairing QR (or enter host 127.0.0.1, the port and the token
  from `herdr-bridge pair`). Ctrl+C stops the bridge.

.EXAMPLE
  .\scripts\run-on-phone.ps1
  .\scripts\run-on-phone.ps1 -Build -Session spike
#>
param(
    [switch]$Build,
    [string]$Session = '',
    [int]$Port = 8787,
    [string]$Serial = '',
    [string]$BridgeRepo = (Join-Path $env:USERPROFILE 'projects\herdr-bridge')
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = (Get-Command adb -ErrorAction Stop).Source }
$apk = Join-Path $repo 'composeApp\build\outputs\apk\debug\composeApp-debug.apk'

if ($Build -or -not (Test-Path $apk)) {
    Write-Host 'Building debug APK...'
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
    Push-Location $repo
    try { & .\gradlew.bat :composeApp:assembleDebug --console=plain -q; if ($LASTEXITCODE) { throw 'Build failed' } }
    finally { Pop-Location }
}

if (-not $Serial) {
    Write-Host 'Waiting for a physical device (enable USB debugging and accept the prompt on the phone)...'
    while (-not $Serial) {
        $Serial = & $adb devices | Select-Object -Skip 1 |
            Where-Object { $_ -match '^(\S+)\s+device$' -and $_ -notmatch '^emulator-' } |
            ForEach-Object { ($_ -split '\s+')[0] } | Select-Object -First 1
        if (-not $Serial) { Start-Sleep -Seconds 2 }
    }
}
$model = (& $adb -s $Serial shell getprop ro.product.model).Trim()
$sdk = [int](& $adb -s $Serial shell getprop ro.build.version.sdk).Trim()
Write-Host "Device: $model ($Serial), API $sdk"
if ($sdk -lt 34) { throw "Herdr App needs Android 14 (API 34) or newer; this device is API $sdk." }

Write-Host 'Installing APK...'
& $adb -s $Serial install -r $apk
if ($LASTEXITCODE) { throw 'adb install failed' }
& $adb -s $Serial reverse "tcp:$Port" "tcp:$Port" | Out-Null
& $adb -s $Serial shell am start -n io.github.vladimirvasilev.herdrapp/.MainActivity | Out-Null

Write-Host ''
Write-Host "Phone -> 127.0.0.1:$Port is forwarded to this PC. Starting herdr-bridge (Ctrl+C to stop)."
Write-Host 'In the app: Hosts -> Scan pairing QR (scan the QR below).'
$bridgeArgs = @('-m', 'uv', 'run', '--project', $BridgeRepo, 'herdr-bridge', 'serve', '--bind', '127.0.0.1', '--port', "$Port")
if ($Session) { $bridgeArgs += @('--session', $Session) }
& python @bridgeArgs
