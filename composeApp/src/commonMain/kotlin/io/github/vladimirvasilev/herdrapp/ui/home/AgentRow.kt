package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.PaneContext
import io.github.vladimirvasilev.herdrapp.ui.components.StatusBadge

/** An agent as a card: what it is and its status, and under that [context], where it is. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AgentRow(pane: Pane, context: String?, onOpen: (String) -> Unit, onLongPress: () -> Unit) {
    val agent = pane.agent
    Card(
        Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .combinedClickable(onClick = { onOpen(pane.paneId) }, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = HerdrTheme.colors.panel),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    agent?.kind ?: stringResource(Res.string.home_agent_unknown_kind),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(pane.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(agent?.status ?: AgentStatus.UNKNOWN)
            }
            if (context != null) {
                Text(
                    context,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = HerdrTheme.colors.muted,
                )
            }
        }
    }
}

/**
 * The line under an agent's title: "workspace / tab · folder". Under a workspace's own heading
 * the location is already on screen, so [withLocation] is false there and only the folder is left.
 */
internal fun PaneContext.line(withLocation: Boolean): String? {
    val location = if (withLocation) listOfNotNull(workspace, tab).joinToString(" / ") else ""
    return listOfNotNull(location.takeIf { it.isNotEmpty() }, folder).joinToString("  ·  ").takeIf { it.isNotEmpty() }
}
