# Herdr App

Herdr App is a mobile-first Android client for the [herdr](https://herdr.dev) terminal multiplexer. It connects to a local `herdr-bridge` WebSocket over Tailscale, lists AI coding-agent panes, opens one pane full-screen, and supports observe/control terminal streaming with quick action keys.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug testDebugUnitTest
```

The Android app id is `io.github.vladimirvasilev.herdrapp`; minSdk is 34.

## Module layout

- `composeApp`: Kotlin Multiplatform app module with Android target, shared protocol models, bridge connection/state, host pairing, ViewModels/state holders, and Compose UI.
- `terminal-emulator`: Apache-2.0 Android library module adapted for remote byte streams.
- `terminal-view`: Apache-2.0 Android library module containing a remote terminal session and Android `View` used from Compose.

## Pairing

Use the add-host screen to scan or paste a URI of the form:

```text
herdr-bridge://pair?host=<host>&port=<port>&token=<token>&name=<display-name>
```

Tokens are stored locally and never logged.

## Terminal attribution

The terminal modules follow Termux's Apache-2.0 terminal module packaging and are adapted for a remote bridge stream instead of spawning a local PTY/JNI subprocess. The upstream project is https://github.com/termux/termux-app (`terminal-emulator` and `terminal-view`). License headers and this attribution are retained for the vendored/adapted sources.
