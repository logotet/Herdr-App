# Herdr App

Herdr App is a mobile-first Android client for the [herdr](https://herdr.dev) terminal multiplexer. It connects to a local `herdr-bridge` WebSocket over Tailscale, lists the workspaces with their tabs and panes, opens one pane full-screen, and supports observe/control terminal streaming with quick action keys.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug testDebugUnitTest
```

The Android app id is `io.github.vladimirvasilev.herdrapp`; minSdk is 34.

## The list

The tabs at the top choose between **Workspaces** (every pane, by workspace and tab) and **Agents** (only the panes that run an agent, the ones waiting for you first). Each tab shows how many it holds, and Agents shows a dot while one is waiting. The app remembers the choice.

An agent's card shows the agent, the pane's title and its status. The line under it says where the agent is: workspace, tab and working folder in the Agents list, and only the folder under a workspace's own heading. A part that would repeat another, or a tab that has only a number for a name, is left out, and a home folder is shown as `~`.

## Changing the layout

On the workspace list, long-press a pane, a tab name or a workspace to rename or close it. Tap **+** on a workspace to add a tab with a shell, and the round **+** at the bottom right to add a workspace in herdr's default folder. These are herdr's `rename`, `close` and `create` calls for panes, tabs and workspaces. Closing stops the programs on the PC and cannot be undone, so the app asks first.

## Using a pane

- **Prompt box** (under a pane that runs an agent; a shell or an editor has none, because the text and its Enter would run there as commands): uses the normal phone keyboard (autocorrect, swipe, voice, paste). Enter or the ➤ button sends the text plus Enter to the pane through herdr `pane.send_input`. This works without taking control, so the PC pane is never resized. To add a new line, long-press ➤ or use Shift+Enter on a hardware keyboard. Pasted multi-line text arrives as one prompt in Claude Code and Copilot CLI. Sending an empty box just presses Enter. Each pane keeps its own draft while you swipe between agents.
- **Scrolling**: swipe down on the pane to open **History**. The app loads the pane's recent output with herdr `pane.read` (`recent_unwrapped`, ANSI, up to 1000 rows), so lines re-wrap to the phone width. You scroll it locally, and the PC view never moves. Long-press selects text to copy. Tap **↓ Live**, keep scrolling down past the end, or send a prompt to return to the live stream. Full-screen apps (for example Copilot CLI) keep no herdr scrollback; use PgUp/PgDn there.
- **Take control** (the round keyboard button): raw terminal keyboard for TUI keys. This resizes the real PC pane while you hold control. The button turns red while you hold it; tap it to release.
- **Key rows**: two rows of keys under the pane, one set for agents and one for other panes. They are sent without taking control. **Ctrl** and **Alt** apply to the next key, from the rows or from the phone keyboard; a second tap locks them.
- **Swiping**: from an agent, left and right go through all agents. From any other pane they stay inside its workspace.

## Notifications

Off by default. **Stay connected and notify** on the Hosts screen keeps the bridge connection while the app is off the screen and posts a notification when an agent starts waiting for you or finishes. Tapping it opens that agent's pane. Waiting and finished are separate Android notification channels, so either can be silenced in the system settings.

The connection is kept by a foreground service, which is why Android shows a permanent "Connected to" notification meanwhile. It only runs while there is something to wait for: ten minutes after the last agent stopped working or waiting, or after the bridge became unreachable, it stops, and it starts again the next time the app is opened. A task started later from the PC therefore does not notify until then. The app holds no wake lock, so in deep sleep a notification can arrive late. While the app is off the screen it closes its pane streams and releases control.

## Module layout

- `composeApp`: Kotlin Multiplatform app module with an Android target. `domain` holds the models and repository interfaces, `data` the bridge connection and storage, and `ui` one package per screen with a ViewModel each. `AGENTS.md` has the rules.
- `terminal-emulator`: the Termux VT emulator (Apache-2.0, vendored), with `TerminalSession` rewritten to be fed by herdr frames from the bridge instead of a local PTY.
- `terminal-view`: the Termux `TerminalView` Android `View` (Apache-2.0, vendored), hosted in Compose via `AndroidView` in `TerminalPane.android.kt`.

## Pairing

Use the add-host screen to scan or paste a URI of the form:

```text
herdr-bridge://pair?host=<host>&port=<port>&token=<token>&name=<display-name>
```

Tokens are stored locally, never logged, and excluded from cloud backup and device-to-device transfer, so a restored phone has to be paired again.

## Testing on a USB-connected phone (no Tailscale)

```powershell
.\scripts\run-on-phone.ps1 [-Build] [-Session <herdr-session>] [-Serial <adb-serial>]
```

The script installs the debug APK and sets up `adb reverse tcp:8787`. It then runs
`herdr-bridge serve --bind 127.0.0.1` in the foreground. In the app, add a host with
`127.0.0.1:8787` and the token from `%APPDATA%\herdr-bridge\config.toml`, or scan the
printed QR. This works only while the phone is plugged in.

## Terminal attribution

`terminal-emulator` and `terminal-view` are vendored from [termux/termux-app](https://github.com/termux/termux-app) at commit `8629e63`. Upstream licenses these two modules under the Apache License 2.0; the rest of termux-app is GPLv3 and is not used. The sources are kept in Java and as close to upstream as possible, so upstream fixes can be diffed in. Each module's `README.md` lists the Herdr-specific changes: the remote-backed `TerminalSession`, the removed JNI/PTY code, and small `TerminalView` scroll hooks. The upstream emulator unit tests are included and run with `testDebugUnitTest`.
