# Herdr App

Herdr App is a mobile-first Android client for the [herdr](https://herdr.dev) terminal multiplexer. It connects to a local `herdr-bridge` WebSocket over Tailscale, lists AI coding-agent panes, opens one pane full-screen, and supports observe/control terminal streaming with quick action keys.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug testDebugUnitTest
```

The Android app id is `io.github.vladimirvasilev.herdrapp`; minSdk is 34.

## Module layout

- `composeApp`: Kotlin Multiplatform app module with Android target, shared protocol models, bridge connection/state, host pairing, ViewModels/state holders, and Compose UI.
- `terminal-emulator`: the Termux VT emulator (Apache-2.0, vendored), with `TerminalSession` rewritten to be fed by herdr frames from the bridge instead of a local PTY.
- `terminal-view`: the Termux `TerminalView` Android `View` (Apache-2.0, vendored), hosted in Compose via `AndroidView` in `TerminalPane.android.kt`.

## Pairing

Use the add-host screen to scan or paste a URI of the form:

```text
herdr-bridge://pair?host=<host>&port=<port>&token=<token>&name=<display-name>
```

Tokens are stored locally and never logged.

## Testing on a USB-connected phone (no Tailscale)

```powershell
.\scripts\run-on-phone.ps1 [-Build] [-Session <herdr-session>] [-Serial <adb-serial>]
```

The script installs the debug APK and sets up `adb reverse tcp:8787`. It then runs
`herdr-bridge serve --bind 127.0.0.1` in the foreground. In the app, add a host with
`127.0.0.1:8787` and the token from `%APPDATA%\herdr-bridge\config.toml`, or scan the
printed QR. This works only while the phone is plugged in.

## Terminal attribution

`terminal-emulator` and `terminal-view` are vendored from [termux/termux-app](https://github.com/termux/termux-app) at commit `8629e63`. Upstream licenses these two modules under the Apache License 2.0; the rest of termux-app is GPLv3 and is not used. The sources are kept in Java and as close to upstream as possible, so upstream fixes can be diffed in. Each module's `README.md` lists the Herdr-specific changes: the remote-backed `TerminalSession`, the removed JNI/PTY code, and two small `TerminalView` hooks. The upstream emulator unit tests are included and run with `testDebugUnitTest`.
