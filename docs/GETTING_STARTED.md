# Getting started with Herdr App and herdr-bridge

This guide takes a new user from nothing to a phone that shows and drives the terminals on a PC.

## 1. What you are setting up

| Part | Runs on | Job |
|---|---|---|
| [herdr](https://herdr.dev) | PC | Runs your terminals and agents. |
| herdr-bridge | The same PC | Opens herdr to the network, protected by a token. |
| Herdr App | Android phone | Shows workspaces and agents, opens panes, sends input. |

The phone talks only to the bridge. The bridge talks to herdr locally.

## 2. Requirements

**PC**
- herdr 0.8.0-preview or newer, running.
- Python 3.12 or newer and [uv](https://docs.astral.sh/uv/).
- `git` on PATH, for the diff feature.

**Network**, one of:
- Tailscale on both the PC and the phone. Recommended: the connection is encrypted.
- A LAN address the phone can reach, on a network you trust. The token travels unencrypted.

**Phone**
- Android 14 or newer.
- The app installed. There is no published release yet, so build the APK from this repo (see
  `README.md`) and install it.

## 3. Install the bridge, once per PC

```powershell
git clone https://github.com/logotet/Herdr-Bridge.git
cd Herdr-Bridge
uv sync
uv run herdr-bridge check
```

`check` confirms that the bridge can reach herdr and prints its version. Fix this before going on.

## 4. Choose how the phone reaches the PC

- **Tailscale:** nothing to configure. By default the bridge listens only on the PC's Tailscale
  address.
- **LAN:** set `bind` in `%APPDATA%\herdr-bridge\config.toml` to the PC's fixed LAN address, and
  allow the bridge through Windows Firewall when Windows asks. Never use `0.0.0.0`.

## 5. Pair the phone

On the PC:

```powershell
uv run herdr-bridge pair
```

This creates the token on first use and prints a QR code that holds the address, the port, the
token and the PC's name.

On the phone:

1. Open the app and tap **Hosts**.
2. Add a host by scanning the QR code, or type the address, the port and the token by hand.

## 6. Start the bridge

```powershell
uv run herdr-bridge serve          # in the foreground, Ctrl+C to stop
uv run herdr-bridge install-task   # or: start hidden at every logon
```

The banner at the top of the app turns green and reads "Connected to" followed by the PC's name.

## 7. Daily use

**The list**
- The switch at the top chooses **Workspaces** (every pane, by workspace and tab) or **Agents**
  (agent panes only, the ones waiting for you first).
- Pull down to refresh.
- Long-press a pane, a tab name or a workspace to rename or close it. **+** on a workspace adds a
  tab; **+** at the top adds a workspace.

**A pane**
- It opens read-only and live. Swipe sideways for the next pane.
- **Prompt box**, on agent panes: type and send without taking control.
- **Key rows**: Esc, arrows, Tab, Ctrl and so on, sent without taking control.
- **Green keyboard button**: take control for raw typing. This resizes the pane on the PC. The
  button turns red; tap it to release.
- **Pull down** on the pane for a scrollable History.

## 8. More devices

- **Another phone, same PC:** run `pair` again and scan. It is the same token.
- **Another PC:** repeat sections 3 to 6 there. That PC gets its own token, and the phone saves it
  as a second host.

## 9. Security

- The token is the only lock. Anyone who has it and can reach the bridge has full control of your
  terminals.
- Treat the QR code as a password.
- If it leaks: `uv run herdr-bridge rotate-token`, restart the bridge, and pair every phone again.

## 10. If it does not connect

| Banner in the app | Likely cause |
|---|---|
| "Connecting…" that never ends | The phone cannot reach the address: wrong network, the PC's Wi-Fi is off, or the firewall blocks it. |
| "…expected status code 101 but was 401" | Wrong token. Pair again. |
| "herdr unavailable" | The bridge is up, but herdr is not running on the PC. |

The bridge writes its log to `%APPDATA%\herdr-bridge\bridge.log`.

Known limits: the app disconnects when it goes to the background, and outside Tailscale the token
is sent unencrypted.
