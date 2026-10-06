# terminal-view

Vendored from [termux/termux-app](https://github.com/termux/termux-app/tree/8629e632fcb95da272221be327db653fb24befe9/terminal-view)
at commit `8629e63`. These modules are licensed under the **Apache License 2.0** (see the
upstream `LICENSE.md` exception for `terminal-emulator` and `terminal-view`). The rest of termux-app is GPLv3 and is not used.

Modifications for Herdr App (`TerminalView.java` only):
- `setRemoteScrollListener()`: scroll gestures go to the remote pane instead of the local transcript.
- `onCheckIsTextEditor()` returns true only while the session accepts input (control mode).
