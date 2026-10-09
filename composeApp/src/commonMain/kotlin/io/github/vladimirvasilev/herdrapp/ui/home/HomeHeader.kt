package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import herdrapp.composeapp.generated.resources.*
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.HomeView
import io.github.vladimirvasilev.herdrapp.ui.components.StatusDot
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The tabs for the two lists, each with how many entries it has. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeHeader(
    view: HomeView,
    workspaceCount: Int,
    agentCount: Int,
    /** An agent is waiting for the user; shown as a dot on the Agents tab. */
    agentWaiting: Boolean,
    onView: (HomeView) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = HomeView.entries.indexOf(view),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        HomeView.entries.forEach { entry ->
            Tab(
                selected = entry == view,
                onClick = { onView(entry) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    TabLabel(
                        label = stringResource(entry.label()),
                        count = if (entry == HomeView.WORKSPACES) workspaceCount else agentCount,
                        waiting = entry == HomeView.AGENTS && agentWaiting,
                    )
                },
            )
        }
    }
}

@Composable
private fun TabLabel(label: String, count: Int, waiting: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(count.toString(), color = HerdrTheme.colors.muted)
        if (waiting) StatusDot(AgentStatus.BLOCKED)
    }
}

/** The round button that adds a workspace; it floats over the workspace list. */
@Composable
internal fun NewWorkspaceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            painterResource(Res.drawable.ic_add),
            contentDescription = stringResource(Res.string.home_new_workspace),
        )
    }
}

private fun HomeView.label() = when (this) {
    HomeView.WORKSPACES -> Res.string.home_view_workspaces
    HomeView.AGENTS -> Res.string.home_view_agents
}
