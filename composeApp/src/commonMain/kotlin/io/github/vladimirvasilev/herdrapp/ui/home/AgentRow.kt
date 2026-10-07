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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.protocol.AgentInfo
import io.github.vladimirvasilev.herdrapp.protocol.PaneInfo
import io.github.vladimirvasilev.herdrapp.ui.components.StatusBadge

@Composable
internal fun AgentRow(agent: AgentInfo, preview: String?, onOpen: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onOpen(agent.paneId) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181825)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(agent.agent ?: "agent", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(agent.title(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(agent.agentStatus)
            }
            if (!preview.isNullOrBlank()) {
                Text(
                    preview.lines().filter { it.isNotBlank() }.takeLast(3).joinToString("\n"),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCDD6F4),
                )
            }
        }
    }
}

@Composable
internal fun OtherPanes(panes: List<PaneInfo>) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF11111B), RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Text("Other panes (${panes.size})", color = Color.Gray)
        AnimatedVisibility(expanded) {
            Column {
                panes.forEach { pane ->
                    Text(
                        "? ${pane.terminalTitleStripped ?: pane.label ?: pane.paneId}",
                        color = Color.Gray,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
