# terminal-emulator

Vendored from [termux/termux-app](https://github.com/termux/termux-app/tree/8629e632fcb95da272221be327db653fb24befe9/terminal-emulator)
at commit `8629e63`. These modules are licensed under the **Apache License 2.0** (see the
upstream `LICENSE.md` exception for `terminal-emulator` and `terminal-view`). The rest of termux-app is GPLv3 and is not used.

Modifications for Herdr App:
- `TerminalSession` is rewritten to be backed by a remote byte stream (herdr frames via herdr-bridge) instead of a local JNI/PTY process. It keeps the public API used by `TerminalView`/`TerminalEmulator` and adds `RemoteIO`, `appendRemote()` and `setInputEnabled()`.
- `JNI.java` and the native `jni/` sources are removed.
- Upstream unit tests are included under `src/test`.
