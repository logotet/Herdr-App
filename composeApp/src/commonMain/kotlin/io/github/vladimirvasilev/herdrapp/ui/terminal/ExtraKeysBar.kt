package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme

@Composable
internal fun ExtraKeysBar(modifiers: KeyModifiers, onKey: (ExtraKey) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(HerdrTheme.colors.panelInset)
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EXTRA_KEYS.forEach { key ->
            AssistChip(
                onClick = { onKey(key) },
                label = { Text(key.capLabel(modifiers)) },
            )
        }
    }
}

/** A modifier shows that it is on in capitals, with a star while it is locked. */
private fun ExtraKey.capLabel(modifiers: KeyModifiers): String {
    if (this !is ExtraKey.Modifier) return label
    return when (modifiers.state(modifier)) {
        ModifierState.OFF -> label
        ModifierState.ONCE -> label.uppercase()
        ModifierState.LOCKED -> label.uppercase() + "*"
    }
}
