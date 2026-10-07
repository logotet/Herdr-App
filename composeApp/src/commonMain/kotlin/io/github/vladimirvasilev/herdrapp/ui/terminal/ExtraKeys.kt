package io.github.vladimirvasilev.herdrapp.ui.terminal

/** What a key press sends to the pane. */
internal sealed interface KeySpec {
    data class Bytes(val bytes: ByteArray, val textFallback: String) : KeySpec
    data class HerdrKey(val key: String) : KeySpec
}

enum class KeyModifier { CTRL, ALT }

enum class ModifierState {
    OFF,
    /** Applies to the next key, then switches off. */
    ONCE,
    /** Stays on until it is tapped again. */
    LOCKED;

    val active: Boolean get() = this != OFF
}

/** The Ctrl and Alt toggles of one pane. They apply to the key rows and to the phone keyboard. */
data class KeyModifiers(
    val ctrl: ModifierState = ModifierState.OFF,
    val alt: ModifierState = ModifierState.OFF,
) {
    fun state(modifier: KeyModifier): ModifierState = when (modifier) {
        KeyModifier.CTRL -> ctrl
        KeyModifier.ALT -> alt
    }

    /** A tap moves a modifier from off to one key, a second tap locks it, a third switches it off. */
    fun tapped(modifier: KeyModifier): KeyModifiers = when (modifier) {
        KeyModifier.CTRL -> copy(ctrl = ctrl.next())
        KeyModifier.ALT -> copy(alt = alt.next())
    }

    /** A key was sent: the one-shot modifiers are used up. */
    fun afterKey(): KeyModifiers = copy(ctrl = ctrl.used(), alt = alt.used())

    private fun ModifierState.next() = when (this) {
        ModifierState.OFF -> ModifierState.ONCE
        ModifierState.ONCE -> ModifierState.LOCKED
        ModifierState.LOCKED -> ModifierState.OFF
    }

    private fun ModifierState.used() = if (this == ModifierState.ONCE) ModifierState.OFF else this
}

/** One key cap of the extra-keys rows. Labels name keys and are not translated. */
internal sealed interface ExtraKey {
    val label: String

    data class Modifier(override val label: String, val modifier: KeyModifier) : ExtraKey

    /** A key herdr knows by [name], for example "esc" or "pageup". */
    data class Named(override val label: String, val name: String) : ExtraKey

    /** A printable character, typed as it is. */
    data class Text(override val label: String) : ExtraKey
}

/**
 * What [key] sends while [modifiers] are held; null for a modifier key, which sends nothing.
 * A combination is always sent as a herdr key name such as "ctrl+up", which needs no control.
 */
internal fun resolve(key: ExtraKey, modifiers: KeyModifiers): KeySpec? {
    val prefix = buildString {
        if (modifiers.ctrl.active) append("ctrl+")
        if (modifiers.alt.active) append("alt+")
    }
    return when (key) {
        is ExtraKey.Modifier -> null
        is ExtraKey.Named -> KeySpec.HerdrKey(prefix + key.name)
        is ExtraKey.Text ->
            if (prefix.isEmpty()) {
                KeySpec.Bytes(key.label.encodeToByteArray(), key.label)
            } else {
                KeySpec.HerdrKey(prefix + key.label.lowercase())
            }
    }
}

private val ESC = ExtraKey.Named("Esc", "esc")
private val TAB = ExtraKey.Named("Tab", "tab")
private val ENTER = ExtraKey.Named("Enter", "enter")
private val CTRL = ExtraKey.Modifier("Ctrl", KeyModifier.CTRL)
private val ALT = ExtraKey.Modifier("Alt", KeyModifier.ALT)
private val UP = ExtraKey.Named("↑", "up")
private val DOWN = ExtraKey.Named("↓", "down")
private val LEFT = ExtraKey.Named("←", "left")
private val RIGHT = ExtraKey.Named("→", "right")

/** Two rows of seven keys. The arrows sit in the same cells in both sets. */
internal val AGENT_KEYS: List<List<ExtraKey>> = listOf(
    listOf(ESC, ExtraKey.Named("S-Tab", "shift+tab"), ExtraKey.Text("/"), ExtraKey.Text("1"), UP, ExtraKey.Text("2"), ExtraKey.Text("3")),
    listOf(TAB, CTRL, ExtraKey.Named("^C", "ctrl+c"), LEFT, DOWN, RIGHT, ENTER),
)

/** For a pane without an agent: an editor, a file manager, a shell. */
internal val PANE_KEYS: List<List<ExtraKey>> = listOf(
    listOf(ESC, ExtraKey.Text(":"), ExtraKey.Text("/"), ENTER, UP, ExtraKey.Named("End", "end"), ExtraKey.Named("PgUp", "pageup")),
    listOf(TAB, CTRL, ALT, LEFT, DOWN, RIGHT, ExtraKey.Named("PgDn", "pagedown")),
)
