package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onDialog: (HomeDialog) -> Unit,
) {
    val workspaceId = group.workspace.id
    item(key = "ws-$workspaceId") {
        val workspace = group.workspace
        LongPressMenu(
            name = workspace.label,
            onRename = { onDialog(HomeDialog.RenameWorkspace(workspaceId, workspace.label)) },
            onClose = { onDialog(HomeDialog.CloseWorkspace(workspaceId, workspace.label, workspace.paneCount)) },
        ) { onLongPress ->
            WorkspaceHeader(
                group = group,
                collapsed = collapsed,
                onToggle = onToggle,
                onLongPress = onLongPress,
                onNewTab = { onDialog(HomeDialog.NewTab(workspaceId, workspace.label)) },
            )
        }
    }
    if (collapsed) return
    group.tabs.forEach { tabGroup ->
        val tab = tabGroup.tab
        if (tab != null && group.tabs.size > 1) {
            item(key = "tab-${tab.id}") { TabHeader(tab, onDialog) }
        }
        items(tabGroup.panes, key = { it.paneId }) { pane -> PaneEntry(pane, onOpen, onDialog) }
    }
}

/** A pane of the list with its long-press menu: an agent as a card, anything else as a row. */
@Composable
internal fun PaneEntry(pane: Pane, onOpen: (String) -> Unit, onDialog: (HomeDialog) -> Unit) {
    LongPressMenu(
        name = pane.title,
        onRename = { onDialog(HomeDialog.RenamePane(pane.paneId, pane.title)) },
        onClose = { onDialog(HomeDialog.ClosePane(pane.paneId, pane.title)) },
    ) { onLongPress ->
        if (pane.agent != null) AgentRow(pane, onOpen, onLongPress) else PaneRow(pane, onOpen, onLongPress)
    }
}

/** The menu starts with [name], because it can open over a neighbouring row. */
@Composable
private fun LongPressMenu(
    name: String,
    onRename: () -> Unit,
    onClose: () -> Unit,
    content: @Composable (onLongPress: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        content { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                name,
                style = MaterialTheme.typography.labelMedium,
                color = HerdrTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 240.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_rename)) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_close), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    open = false
                    onClose()
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkspaceHeader(
    group: WorkspaceGroup,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onNewTab: () -> Unit,
) {
    val workspace = group.workspace
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onToggle, onLongClick = onLongPress).padding(top = 6.dp),
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
        IconButton(onClick = onNewTab) {
            Icon(
                painterResource(Res.drawable.ic_add),
                contentDescription = stringResource(Res.string.home_new_tab),
                tint = HerdrTheme.colors.muted,
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabHeader(tab: Tab, onDialog: (HomeDialog) -> Unit) {
    val name = tab.label.ifBlank { stringResource(Res.string.home_tab_number, tab.number) }
    LongPressMenu(
        name = name,
        onRename = { onDialog(HomeDialog.RenameTab(tab.id, tab.label)) },
        onClose = { onDialog(HomeDialog.CloseTab(tab.id, name)) },
    ) { onLongPress ->
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = HerdrTheme.colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        )
    }
}

/** A pane that runs no agent: a shell, an editor, a file manager. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaneRow(pane: Pane, onOpen: (String) -> Unit, onLongPress: () -> Unit) {
    Text(
        pane.title,
        color = HerdrTheme.colors.terminalText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HerdrTheme.colors.panelInset)
            .combinedClickable(onClick = { onOpen(pane.paneId) }, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
