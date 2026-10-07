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
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.ui.components.StatusBadge

@Composable
internal fun TerminalTopBar(
    agent: Agent,
    controlling: Boolean,
    onBack: () -> Unit,
    onRelease: () -> Unit,
    onTakeControl: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(HerdrTheme.colors.panel).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(stringResource(Res.string.terminal_back)) }
        Column(Modifier.weight(1f)) {
            Text(agent.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                agent.kind ?: agent.paneId,
                color = HerdrTheme.colors.muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        StatusBadge(agent.status)
        Spacer(Modifier.width(8.dp))
        if (controlling) {
            Button(onClick = onRelease) { Text(stringResource(Res.string.terminal_release)) }
        } else {
            Button(onClick = onTakeControl) { Text(stringResource(Res.string.terminal_take_control)) }
        }
    }
}

/** Shown over the pane while the local scrollback replaces the live stream. */
@Composable
internal fun BoxScope.HistoryOverlay(onLive: () -> Unit) {
    Button(onClick = onLive, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
        Text(stringResource(Res.string.terminal_live))
    }
    Text(
        stringResource(Res.string.terminal_history),
        color = HerdrTheme.colors.onHistoryBadge,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .background(HerdrTheme.colors.historyBadge, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
internal fun BoxScope.Notice(text: String) {
    Text(
        text,
        color = HerdrTheme.colors.onNotice,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(12.dp)
            .background(HerdrTheme.colors.notice, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun TakeControlDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.terminal_take_control_title)) },
        text = { Text(stringResource(Res.string.terminal_take_control_body)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.terminal_take_control)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.terminal_cancel)) } },
    )
}
