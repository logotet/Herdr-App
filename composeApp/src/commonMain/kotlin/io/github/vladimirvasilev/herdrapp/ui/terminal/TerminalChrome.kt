package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentInfo
import io.github.vladimirvasilev.herdrapp.ui.components.StatusBadge

@Composable
internal fun TerminalTopBar(
    agent: AgentInfo,
    controlling: Boolean,
    onBack: () -> Unit,
    onRelease: () -> Unit,
    onTakeControl: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF181825)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Column(Modifier.weight(1f)) {
            Text(agent.title(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(agent.agent ?: agent.paneId, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        StatusBadge(agent.agentStatus)
        Spacer(Modifier.width(8.dp))
        if (controlling) {
            Button(onClick = onRelease) { Text("Release") }
        } else {
            Button(onClick = onTakeControl) { Text("Take control") }
        }
    }
}

/** Shown over the pane while the local scrollback replaces the live stream. */
@Composable
internal fun BoxScope.HistoryOverlay(onLive: () -> Unit) {
    Button(onClick = onLive, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) { Text("↓ Live") }
    Text(
        "History",
        color = Color.Black,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .background(Color(0xFFF9E2AF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
internal fun BoxScope.Notice(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(12.dp)
            .background(Color(0xE6313244), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun TakeControlDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take control?") },
        text = { Text("Control mode sends raw keyboard input and resizes the real PC pane.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Take control") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
