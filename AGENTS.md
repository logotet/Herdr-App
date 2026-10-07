# Herdr App: agent guidelines

Android client for the herdr terminal multiplexer. These rules apply to every AI agent and
human working in this repo. When a rule and the existing code disagree, the rule wins for new
code; do not copy the old pattern.

## How to work

- Do what was asked and nothing else. No drive-by refactors, renames or reformatting.
- If a task needs a new dependency, a change to vendored code, or a change to the wire
  protocol, say so and ask before doing it.
- Before reporting a task as done, run the build and tests below and report the real result.
- Delete code that becomes unused. Do not add models, requests or flows "for later".
- Keep `README.md` and the vendored module READMEs true in the same commit as the change.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug testDebugUnitTest
```

## Architecture

`ui` and `data` both depend on `domain`. `domain` depends on neither, and `ui` never imports `data`.

- `data/`: the bridge connection, protocol DTOs (`@Serializable`), DataStore. DTOs never leave
  this layer; map them to domain models at the repository boundary.
- `domain/`: plain Kotlin models and logic (for example agent ordering). No Compose, no Ktor,
  no serialization annotations.
- `ui/<feature>/`: one package per screen (`home`, `hosts`, `terminal`), each with
  `XScreen.kt`, `XViewModel.kt` and `XUiState.kt`.
- Wiring lives in `HerdrApplication`. Pass dependencies through constructors.

- Screens are destinations in `ui/navigation/HerdrNavHost.kt`, with typed `@Serializable` routes.
  A route carries ids only, never a model.

## UI rules

- One ViewModel per screen, exposing a single `StateFlow<XUiState>`. Composables receive state
  and event lambdas. They never receive a repository, `BridgeConnection` or `HerdrStore`.
- Network and storage calls start in the ViewModel scope, never in `rememberCoroutineScope`.
- State that must survive rotation or process death lives in the ViewModel or
  `rememberSaveable`. Navigation state is never a plain `remember`.
- One statement per line. No composable written as a single long line. Files stay under about
  200 lines; split by component when they grow.
- Colors come from `MaterialTheme` or `HerdrTheme.colors`, and user-visible text from Compose
  resources (`composeResources/values/strings.xml`). No inline hex colors or prose literals in
  composables. ViewModels expose what happened as a type; the composable picks the text.
  Key cap labels and glyphs are not prose.

## Coroutines and connection

- JSON parsing and Base64 decoding run off the main thread.
- Mutable state shared between coroutines is confined to one dispatcher or guarded by a `Mutex`.
  Everything that touches the socket runs on the single-threaded bridge dispatcher.
- A `BridgeListener` runs inside the loop that reads the socket. It must never wait for the
  result of a request, or the result can never be read.
- Never drop terminal frames: no lossy buffer between the socket and the emulator. Frames of a
  stream are numbered 1, 2, 3; a delta that does not follow its predecessor is not drawn and the
  stream is restarted, which brings a full frame.
- Always rethrow `CancellationException`.

## Kotlin Multiplatform

Android is the only target today. Keep platform-free code in `commonMain`, but do not add new
`expect`/`actual` declarations until a second target exists.

## Vendored Termux modules

`terminal-emulator` and `terminal-view` are vendored from termux-app (Apache-2.0).

- Prefer solving a problem in `composeApp`. Change vendored code only when there is no other way.
- Mark every change with a `// Herdr App:` comment and list it in that module's `README.md`.
- An upstream import is its own commit containing only pristine upstream files. Local
  modifications come in later commits, so they can be diffed against upstream.

## Tests

- New logic in `domain` and `data` comes with unit tests in the same commit.
- A bug fix comes with a test that fails without the fix, where that is practical.

## Security

- Never log tokens or put them in error messages.
- Tokens must not be included in backups.

## Commits

- Work on a branch (`feat/...`, `fix/...`, `chore/...`), not on `master`.
- Conventional Commits: `type(scope): summary`, imperative mood, at most 72 characters.
  Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`.
- One concern per commit. If the summary needs "and", or the body needs a bullet list of
  unrelated changes, split it.
- Refactors and behavior changes go in separate commits.
- Every commit builds and passes tests.
- The body says why the change was needed, not what the diff already shows.
- Do not end a task with uncommitted changes left in the working tree, and do not commit
  unless asked.
