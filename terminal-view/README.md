# terminal-view

Vendored from [termux/termux-app](https://github.com/termux/termux-app/tree/8629e632fcb95da272221be327db653fb24befe9/terminal-view)
at commit `8629e63`. These modules are licensed under the **Apache License 2.0** (see the
upstream `LICENSE.md` exception for `terminal-emulator` and `terminal-view`). The rest of termux-app is GPLv3 and is not used.

Modifications for Herdr App (`TerminalView.java` only):
- `setRemoteScrollListener()`: scroll gestures go to the app instead of the local transcript (the live view uses this to open history).
- `setScrollPastBottomListener()`: called when the user keeps scrolling down at the bottom of the local transcript (leaves history).
- `onCheckIsTextEditor()` returns true only while the session accepts input (control mode).
- Per-gesture axis lock: vertical drags and pinches are claimed from the parent (the pane pager); horizontal drags are left to it unless zoomed content can pan.
- `setRemoteScrollThreshold()`: a touch drag is reported to the `RemoteScrollListener` once per gesture, after this distance.
- `setFitToWidth()`: a grid wider than the view is scaled down to fit; pinch or double tap zooms, a drag pans.
