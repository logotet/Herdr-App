package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.state.HerdrStore

@Composable
internal fun TerminalPagerScreen(
    initialPage: Int,
    store: HerdrStore,
    connection: BridgeConnection,
    settings: SettingsRepository,
    onBack: () -> Unit,
) {
    val agents by store.agents.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (agents.size - 1).coerceAtLeast(0)),
        pageCount = { agents.size.coerceAtLeast(1) },
    )
    if (agents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text("No agents. Back") }
        }
        return
    }
    val drafts = remember { mutableStateMapOf<String, TextFieldValue>() }
    // History is a reading mode: paging is off while the current pane shows it (leave via ↓ Live).
    val historyOpen = remember { mutableStateMapOf<String, Boolean>() }
    val currentPaneId = agents.getOrNull(pagerState.currentPage)?.paneId
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = historyOpen[currentPaneId] != true,
    ) { page ->
        val agent = agents[page.coerceAtMost(agents.lastIndex)]
        TerminalScreen(
            agent = agent,
            store = store,
            connection = connection,
            settings = settings,
            onBack = onBack,
            draft = drafts[agent.paneId] ?: TextFieldValue(""),
            onDraftChange = { drafts[agent.paneId] = it },
            onHistoryOpenChange = { open ->
                if (open) historyOpen[agent.paneId] = true else historyOpen.remove(agent.paneId)
            },
        )
    }
}
