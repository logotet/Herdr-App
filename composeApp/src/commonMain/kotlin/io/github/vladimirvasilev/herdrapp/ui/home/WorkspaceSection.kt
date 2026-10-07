package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import herdrapp.composeapp.generated.resources.*
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.Tab
import io.github.vladimirvasilev.herdrapp.domain.WorkspaceGroup
import io.github.vladimirvasilev.herdrapp.ui.components.StatusDot
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The chevron drawable points left: turned to point down when open, right when collapsed.
private const val CHEVRON_OPEN = -90f
private const val CHEVRON_COLLAPSED = 180f

/** One workspace: its header, then its panes by tab unless it is collapsed. */
internal fun LazyListScope.workspaceSection(
    group: WorkspaceGroup,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onOpen: (paneId: String) -> Unit,
) {
    val workspaceId = group.workspace.id
    item(key = "ws-$workspaceId") { WorkspaceHeader(group, collapsed, onToggle) }
    if (collapsed) return
    group.tabs.forEach { tabGroup ->
        val tab = tabGroup.tab
        if (tab != null && group.tabs.size > 1) {
            item(key = "tab-${tab.id}") { TabHeader(tab) }
        }
        items(tabGroup.panes, key = { it.paneId }) { pane ->
            if (pane.agent != null) AgentRow(pane, onOpen) else PaneRow(pane, onOpen)
        }
    }
}

@Composable
private fun WorkspaceHeader(group: WorkspaceGroup, collapsed: Boolean, onToggle: () -> Unit) {
    val workspace = group.workspace
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        group.status?.let { status ->
            StatusDot(status)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            stringResource(Res.string.home_workspace, workspace.number, workspace.label, workspace.paneCount),
            style = MaterialTheme.typography.titleSmall,
            color = HerdrTheme.colors.workspaceLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(Res.drawable.ic_chevron_left),
            contentDescription = stringResource(
                if (collapsed) Res.string.home_workspace_expand else Res.string.home_workspace_collapse,
            ),
            tint = HerdrTheme.colors.muted,
            modifier = Modifier.rotate(if (collapsed) CHEVRON_COLLAPSED else CHEVRON_OPEN),
        )
    }
}

@Composable
private fun TabHeader(tab: Tab) {
    Text(
        tab.label.ifBlank { stringResource(Res.string.home_tab_number, tab.number) },
        style = MaterialTheme.typography.labelMedium,
        color = HerdrTheme.colors.muted,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/** A pane that runs no agent: a shell, an editor, a file manager. */
@Composable
private fun PaneRow(pane: Pane, onOpen: (String) -> Unit) {
    Text(
        pane.title,
        color = HerdrTheme.colors.terminalText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .background(HerdrTheme.colors.panelInset, RoundedCornerShape(10.dp))
            .clickable { onOpen(pane.paneId) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
