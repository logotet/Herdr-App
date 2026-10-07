package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.ui.components.StatusBadge

@Composable
internal fun AgentRow(pane: Pane, onOpen: (String) -> Unit) {
    val agent = pane.agent
    Card(
        Modifier.fillMaxWidth().clickable { onOpen(pane.paneId) },
        colors = CardDefaults.cardColors(containerColor = HerdrTheme.colors.panel),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            val preview = agent?.preview
            if (!preview.isNullOrBlank()) {
                Text(
                    preview.lines().filter { it.isNotBlank() }.takeLast(3).joinToString("\n"),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                    color = HerdrTheme.colors.terminalText,
                )
            }
        }
    }
}

@Composable
internal fun OtherPanes(panes: List<Pane>) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(HerdrTheme.colors.panelInset, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Text(stringResource(Res.string.home_other_panes, panes.size), color = HerdrTheme.colors.muted)
        AnimatedVisibility(expanded) {
            Column {
                panes.forEach { pane ->
                    Text(
                        stringResource(Res.string.home_other_pane, pane.title),
                        color = HerdrTheme.colors.muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
