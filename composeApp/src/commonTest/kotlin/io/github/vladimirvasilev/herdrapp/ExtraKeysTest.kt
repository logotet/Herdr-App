package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.ui.terminal.ExtraKey
import io.github.vladimirvasilev.herdrapp.ui.terminal.KeyModifier
import io.github.vladimirvasilev.herdrapp.ui.terminal.KeyModifiers
import io.github.vladimirvasilev.herdrapp.ui.terminal.KeySpec
import io.github.vladimirvasilev.herdrapp.ui.terminal.ModifierState
import io.github.vladimirvasilev.herdrapp.ui.terminal.resolve
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ExtraKeysTest {
    private val ctrl = KeyModifiers(ctrl = ModifierState.ONCE)
    private val up = ExtraKey.Named("Up", "up")

    @Test
    fun tappingAModifierGoesFromOffToOnceToLockedToOff() {
        val once = KeyModifiers().tapped(KeyModifier.ALT)
        val locked = once.tapped(KeyModifier.ALT)

        assertEquals(ModifierState.ONCE, once.alt)
        assertEquals(ModifierState.LOCKED, locked.alt)
        assertEquals(KeyModifiers(), locked.tapped(KeyModifier.ALT))
        assertEquals(ModifierState.OFF, once.ctrl)
    }

    @Test
    fun aKeyUsesUpOneShotModifiersAndKeepsLockedOnes() {
        val modifiers = KeyModifiers(ctrl = ModifierState.ONCE, alt = ModifierState.LOCKED)

        assertEquals(KeyModifiers(alt = ModifierState.LOCKED), modifiers.afterKey())
    }

    @Test
    fun aNamedKeyIsSentByNameWithItsModifiersInFront() {
        val both = KeyModifiers(ctrl = ModifierState.ONCE, alt = ModifierState.LOCKED)

        assertEquals(KeySpec.HerdrKey("up"), resolve(up, KeyModifiers()))
        assertEquals(KeySpec.HerdrKey("ctrl+up"), resolve(up, ctrl))
        assertEquals(KeySpec.HerdrKey("ctrl+alt+up"), resolve(up, both))
    }

    @Test
    fun aCharacterIsTypedUnlessAModifierIsHeld() {
        val typed = assertIs<KeySpec.Bytes>(resolve(ExtraKey.Text("/"), KeyModifiers()))
        assertContentEquals("/".encodeToByteArray(), typed.bytes)
        assertEquals("/", typed.textFallback)

        assertEquals(KeySpec.HerdrKey("ctrl+y"), resolve(ExtraKey.Text("Y"), ctrl))
    }

    @Test
    fun aKeyHerdrCannotNameIsSentAsItsEscapeSequence() {
        fun sent(key: ExtraKey, modifiers: KeyModifiers = KeyModifiers()) =
            assertIs<KeySpec.Bytes>(resolve(key, modifiers)).textFallback

        assertEquals("\u001b[5~", sent(ExtraKey.Sequence("PgUp", 5, '~')))
        assertEquals("\u001b[6~", sent(ExtraKey.Sequence("PgDn", 6, '~')))
        assertEquals("\u001b[F", sent(ExtraKey.Sequence("End", 1, 'F')))
        assertEquals("\u001b[5;5~", sent(ExtraKey.Sequence("PgUp", 5, '~'), ctrl))
        assertEquals("\u001b[1;3F", sent(ExtraKey.Sequence("End", 1, 'F'), KeyModifiers(alt = ModifierState.ONCE)))
    }

    @Test
    fun aModifierKeySendsNothing() {
        assertNull(resolve(ExtraKey.Modifier("Ctrl", KeyModifier.CTRL), KeyModifiers()))
    }
}
