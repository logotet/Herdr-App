package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.ui.components.StatusDot

@Composable
internal fun TerminalTopBar(pane: Pane, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(HerdrTheme.colors.panel).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painterResource(Res.drawable.ic_chevron_left),
                contentDescription = stringResource(Res.string.terminal_back),
            )
        }
        pane.agent?.let { agent ->
            StatusDot(agent.status)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(pane.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pane.agent?.kind ?: pane.paneId,
                color = HerdrTheme.colors.muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Takes control of the pane (green, keyboard) or gives it back (red, stop). */
@Composable
internal fun BoxScope.ControlFab(controlling: Boolean, onTakeControl: () -> Unit, onRelease: () -> Unit) {
    FloatingActionButton(
        onClick = if (controlling) onRelease else onTakeControl,
        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        shape = CircleShape,
        containerColor = if (controlling) HerdrTheme.colors.controlRelease else HerdrTheme.colors.controlTake,
        contentColor = HerdrTheme.colors.onAction,
    ) {
        Icon(
            painterResource(if (controlling) Res.drawable.ic_stop else Res.drawable.ic_keyboard),
            contentDescription = stringResource(
                if (controlling) Res.string.terminal_release else Res.string.terminal_take_control,
            ),
        )
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

/** Shown over a live pane whose view on the PC is scrolled back, which hides all new output. */
@Composable
internal fun BoxScope.ScrolledBackOverlay(lines: Int, canJump: Boolean, onJump: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(HerdrTheme.colors.bannerWarning)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.terminal_scrolled_back, lines),
            color = HerdrTheme.colors.onBannerWarning,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onJump, enabled = canJump) { Text(stringResource(Res.string.terminal_jump_to_latest)) }
    }
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
