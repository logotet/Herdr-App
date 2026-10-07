package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import herdrapp.composeapp.generated.resources.*
import io.github.vladimirvasilev.herdrapp.domain.HomeView
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The switch between the two lists, and the button for a new workspace while workspaces show. */
@Composable
internal fun HomeHeader(view: HomeView, onView: (HomeView) -> Unit, onNewWorkspace: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            HomeView.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = entry == view,
                    onClick = { onView(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, HomeView.entries.size),
                ) {
                    Text(stringResource(entry.label()))
                }
            }
        }
        if (view == HomeView.WORKSPACES) {
            IconButton(onClick = onNewWorkspace) {
                Icon(
                    painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.home_new_workspace),
                )
            }
        } else {
            // Keeps the switch the same width in both views.
            Spacer(Modifier.size(48.dp))
        }
    }
}

private fun HomeView.label() = when (this) {
    HomeView.WORKSPACES -> Res.string.home_view_workspaces
    HomeView.AGENTS -> Res.string.home_view_agents
}
