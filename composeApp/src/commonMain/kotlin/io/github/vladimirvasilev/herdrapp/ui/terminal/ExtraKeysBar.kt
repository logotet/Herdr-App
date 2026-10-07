package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme

private val KEY_SHAPE = RoundedCornerShape(8.dp)

/** The key rows above the phone keyboard. Every key keeps its place, so nothing scrolls. */
@Composable
internal fun ExtraKeysBar(keys: List<List<ExtraKey>>, modifiers: KeyModifiers, onKey: (ExtraKey) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(HerdrTheme.colors.panelInset).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    val state = (key as? ExtraKey.Modifier)?.let { modifiers.state(it.modifier) } ?: ModifierState.OFF
                    KeyCap(key.label, state, onClick = { onKey(key) })
                }
            }
        }
    }
}

/** A modifier that is on is filled; a locked one also carries a bar under its label. */
@Composable
private fun RowScope.KeyCap(label: String, state: ModifierState, onClick: () -> Unit) {
    val content = if (state.active) HerdrTheme.colors.onAction else HerdrTheme.colors.terminalText
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .clip(KEY_SHAPE)
            .background(if (state.active) MaterialTheme.colorScheme.primary else HerdrTheme.colors.panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (state == ModifierState.LOCKED) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .width(18.dp)
                    .height(2.dp)
                    .background(content),
            )
        }
    }
}
