package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.ui.components.ConnectionBanner
import io.github.vladimirvasilev.herdrapp.ui.components.HerdrUnavailableBanner

@Composable
internal fun HomeRoute(container: AppContainer, onOpen: (paneId: String) -> Unit, onHosts: () -> Unit) {
    val viewModel = viewModel { HomeViewModel(container.session, container.hosts) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpen = onOpen,
        onHosts = onHosts,
        onRefresh = viewModel::refresh,
        onDialog = viewModel::showDialog,
    )
    state.dialog?.let { dialog ->
        LayoutDialog(dialog, onConfirm = viewModel::confirmDialog, onDismiss = viewModel::dismissDialog)
    }
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onOpen: (paneId: String) -> Unit,
    onHosts: () -> Unit,
    onRefresh: () -> Unit,
    onDialog: (HomeDialog) -> Unit,
) {
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    // Ids of the workspaces the user folded away.
    var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize()) {
        ConnectionBanner(state.connection, state.hostName, onHosts)
        HerdrUnavailableBanner(state.herdrAvailable)
        state.changeFailed?.let { reason ->
            Text(
                stringResource(Res.string.home_change_failed, reason),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().background(HerdrTheme.colors.bannerError).padding(12.dp),
            )
        }
        if (!state.hasHosts) {
            EmptyHosts(onHosts)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize().pointerInput(Unit) {
                var drag = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (drag > 120f) currentOnRefresh()
                        drag = 0f
                    },
                ) { change, amount ->
                    change.consume()
                    drag += amount.y
                }
            },
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                HeaderRow(
                    title = stringResource(Res.string.home_title),
                    subtitle = stringResource(Res.string.home_refresh_hint),
                    onNewWorkspace = { onDialog(HomeDialog.NewWorkspace) },
                )
            }
            if (state.needsYou.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.home_needs_you)) }
                items(state.needsYou, key = { "needs-${it.paneId}" }) { PaneEntry(it, onOpen, onDialog) }
            }
            state.groups.forEach { group ->
                val id = group.workspace.id
                workspaceSection(
                    group = group,
                    collapsed = id in collapsed,
                    onToggle = { collapsed = if (id in collapsed) collapsed - id else collapsed + id },
                    onOpen = onOpen,
                    onDialog = onDialog,
                )
            }
        }
    }
}

@Composable
private fun EmptyHosts(onHosts: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.home_no_hosts_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.home_no_hosts_body))
            Button(onClick = onHosts) { Text(stringResource(Res.string.home_add_host)) }
        }
    }
}

@Composable
private fun HeaderRow(title: String, subtitle: String, onNewWorkspace: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HerdrTheme.colors.muted)
        IconButton(onClick = onNewWorkspace) {
            Icon(
                painterResource(Res.drawable.ic_add),
                contentDescription = stringResource(Res.string.home_new_workspace),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
}

