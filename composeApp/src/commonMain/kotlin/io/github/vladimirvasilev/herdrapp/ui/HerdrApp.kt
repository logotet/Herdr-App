package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.data.HostRepository
import io.github.vladimirvasilev.herdrapp.data.PairUriParser
import io.github.vladimirvasilev.herdrapp.data.SavedHost
import io.github.vladimirvasilev.herdrapp.data.SettingsRepository
import io.github.vladimirvasilev.herdrapp.network.BridgeConnection
import io.github.vladimirvasilev.herdrapp.protocol.*
import io.github.vladimirvasilev.herdrapp.state.*
import kotlinx.coroutines.launch

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89B4FA),
    secondary = Color(0xFFA6E3A1),
    background = Color(0xFF0C0C0F),
    surface = Color(0xFF15151B),
    error = Color(0xFFF38BA8)
)

@Composable
fun HerdrApp(
    store: HerdrStore,
    connection: BridgeConnection,
    hosts: HostRepository,
    settings: SettingsRepository,
) {
    MaterialTheme(colorScheme = DarkColors) {
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        val hostList by hosts.hosts.collectAsState()
        LaunchedEffect(hostList) {
            val current = connection.currentHost.value
            if (current == null && hostList.isNotEmpty()) connection.connect(hostList.first())
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Edge-to-edge: keep content clear of the status/navigation bars and the keyboard,
            // so the extra-keys bar sits above the IME and the terminal shrinks to fit.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(store, connection, hostList, onOpen = { paneId ->
                    val ordered = store.agents.value
                    val idx = ordered.indexOfFirst { it.paneId == paneId }.coerceAtLeast(0)
                    screen = Screen.Terminal(idx)
                }, onHosts = { screen = Screen.Hosts }, onRefresh = { scope.launch { connection.refresh() } })
                Screen.Hosts -> HostScreen(hostList, onBack = { screen = Screen.Home }, onSave = { scope.launch { hosts.upsert(it); connection.connect(it); screen = Screen.Home } }, onSelect = { connection.connect(it); screen = Screen.Home }, onDelete = { scope.launch { hosts.delete(it.id) } })
                is Screen.Terminal -> TerminalPagerScreen(s.initialPage, store, connection, settings, onBack = { screen = Screen.Home })
            }
            }
        }
    }
}

private sealed interface Screen { data object Home : Screen; data object Hosts : Screen; data class Terminal(val initialPage: Int) : Screen }

@Composable
private fun ConnectionBanner(state: ConnectionState, host: SavedHost?, onHosts: () -> Unit) {
    val text = when (state) {
        ConnectionState.Disconnected -> "Disconnected"
        ConnectionState.Connecting -> "Connecting to ${host?.name ?: "host"}?"
        is ConnectionState.Connected -> "Connected to ${state.bridgeName}"
        is ConnectionState.Error -> "Connection error: ${state.message}"
    }
    val color = when (state) {
        is ConnectionState.Connected -> Color(0xFF23452C)
        is ConnectionState.Error -> Color(0xFF4A2027)
        else -> Color(0xFF303044)
    }
    Row(Modifier.fillMaxWidth().background(color).clickable { onHosts() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("Hosts", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HomeScreen(store: HerdrStore, connection: BridgeConnection, hostList: List<SavedHost>, onOpen: (String) -> Unit, onHosts: () -> Unit, onRefresh: () -> Unit) {
    val state by store.connectionState.collectAsState()
    val workspaces by store.workspaces.collectAsState()
    val agents by store.agents.collectAsState()
    val panes by store.panes.collectAsState()
    val previews by store.previews.collectAsState()
    val currentHost by connection.currentHost.collectAsState()
    val herdrStatus by store.herdrStatus.collectAsState()
    val groups = remember(workspaces, agents, panes) { AgentOrganizer.groups(workspaces, agents, panes) }
    val needsYou = remember(agents) { AgentOrganizer.needsYou(agents) }
    Column(Modifier.fillMaxSize()) {
        ConnectionBanner(state, currentHost, onHosts)
        HerdrUnavailableBanner(herdrStatus)
        if (hostList.isEmpty()) {
            EmptyHosts(onHosts)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    var drag = 0f
                    detectDragGestures(onDragEnd = { if (drag > 120f) onRefresh(); drag = 0f }) { change, amount -> change.consume(); drag += amount.y }
                },
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { HeaderRow("Agents", "Pull down to refresh") }
                if (needsYou.isNotEmpty()) {
                    item { SectionTitle("Needs you") }
                    items(needsYou, key = { "needs-${it.paneId}" }) { AgentRow(it, previews[it.paneId], onOpen) }
                }
                groups.forEach { group ->
                    item { WorkspaceHeader(group.workspace) }
                    items(group.agents, key = { it.paneId }) { AgentRow(it, previews[it.paneId], onOpen) }
                    if (group.otherPanes.isNotEmpty()) item { OtherPanes(group.otherPanes) }
                }
            }
        }
    }
}


@Composable private fun HerdrUnavailableBanner(status: HerdrInfo?) {
    if (status?.available == false) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF5A3418)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("herdr not running on PC", fontWeight = FontWeight.Bold, color = Color(0xFFFFD8A8), modifier = Modifier.weight(1f))
            Text("Bridge connected", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD8A8))
        }
    }
}

@Composable private fun EmptyHosts(onHosts: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No bridge host paired", style = MaterialTheme.typography.titleLarge)
            Text("Add a herdr-bridge host by QR scan or manual entry.")
            Button(onClick = onHosts) { Text("Add host") }
        }
    }
}

@Composable private fun HeaderRow(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary) }

@Composable private fun WorkspaceHeader(ws: WorkspaceInfo) {
    Text("${ws.number}. ${ws.label} (${ws.paneCount})", style = MaterialTheme.typography.titleSmall, color = Color(0xFFBAC2DE), modifier = Modifier.padding(top = 6.dp))
}

@Composable private fun AgentRow(agent: AgentInfo, preview: String?, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(agent.paneId) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF181825))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(agent.agent ?: "agent", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(agent.title(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(agent.agentStatus)
            }
            if (!preview.isNullOrBlank()) {
                Text(preview.lines().filter { it.isNotBlank() }.takeLast(3).joinToString("\n"), maxLines = 3, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, color = Color(0xFFCDD6F4))
            }
        }
    }
}

@Composable private fun StatusBadge(status: AgentStatus) {
    val color = when (status) {
        AgentStatus.BLOCKED -> Color(0xFFF38BA8)
        AgentStatus.WORKING -> Color(0xFF89B4FA)
        AgentStatus.DONE -> Color(0xFFA6E3A1)
        AgentStatus.IDLE -> Color(0xFF9399B2)
        AgentStatus.UNKNOWN -> Color.Transparent
    }
    val transition = rememberInfiniteTransition(label = "working")
    val alpha by transition.animateFloat(0.45f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")
    Box(
        Modifier
            .alpha(if (status == AgentStatus.WORKING) alpha else 1f)
            .then(if (status == AgentStatus.UNKNOWN) Modifier.border(1.dp, Color.Gray, RoundedCornerShape(50)) else Modifier.background(color, RoundedCornerShape(50)))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(status.name.lowercase(), style = MaterialTheme.typography.labelSmall, color = if (status == AgentStatus.UNKNOWN) Color.Gray else Color.Black) }
}

@Composable private fun OtherPanes(panes: List<PaneInfo>) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color(0xFF11111B), RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(10.dp)) {
        Text("Other panes (${panes.size})", color = Color.Gray)
        AnimatedVisibility(expanded) { Column { panes.forEach { Text("? ${it.terminalTitleStripped ?: it.label ?: it.paneId}", color = Color.Gray, maxLines = 1) } } }
    }
}

@Composable
private fun HostScreen(hostList: List<SavedHost>, onBack: () -> Unit, onSave: (SavedHost) -> Unit, onSelect: (SavedHost) -> Unit, onDelete: (SavedHost) -> Unit) {
    val scanner = LocalQrScannerService.current
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Hosts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); TextButton(onClick = onBack) { Text("Done") } } }
        items(hostList, key = { it.id }) { saved ->
            Card(Modifier.fillMaxWidth().clickable { onSelect(saved) }) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(saved.name, fontWeight = FontWeight.Bold); Text("${saved.host}:${saved.port}", color = Color.Gray) }; TextButton(onClick = { onDelete(saved) }) { Text("Delete") } } }
        }
        item { Divider() }
        item { Button(onClick = { scanner.scan { raw -> raw?.let { PairUriParser.parse(it)?.let(onSave) ?: run { error = "Invalid QR" } } } }, modifier = Modifier.fillMaxWidth()) { Text("Scan pairing QR") } }
        item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(token, { token = it }, label = { Text("Token") }, modifier = Modifier.fillMaxWidth()) }
        item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = {
            val p = port.toIntOrNull() ?: 8787
            if (host.isBlank() || token.isBlank()) error = "Host and token are required" else onSave(SavedHost(id = "manual-${host.hashCode()}-$p", name = name.ifBlank { host }, host = host, port = p, token = token))
        }, modifier = Modifier.fillMaxWidth()) { Text("Save host") } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalPagerScreen(initialPage: Int, store: HerdrStore, connection: BridgeConnection, settings: SettingsRepository, onBack: () -> Unit) {
    val agents by store.agents.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (agents.size - 1).coerceAtLeast(0)), pageCount = { agents.size.coerceAtLeast(1) })
    if (agents.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TextButton(onClick = onBack) { Text("No agents. Back") } }; return }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        TerminalScreen(agent = agents[page.coerceAtMost(agents.lastIndex)], store = store, connection = connection, settings = settings, onBack = onBack)
    }
}

@Composable
private fun TerminalScreen(agent: AgentInfo, store: HerdrStore, connection: BridgeConnection, settings: SettingsRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val streams by store.streams.collectAsState()
    val herdrStatus by store.herdrStatus.collectAsState()
    val fontSize by settings.terminalFontSize.collectAsState()
    val stream = streams[agent.paneId]
    val controlling = stream?.mode == StreamMode.CONTROL
    var colsRows by remember(agent.paneId) { mutableStateOf(80 to 24) }
    var confirmTakeover by remember { mutableStateOf(false) }

    LaunchedEffect(agent.paneId, colsRows) { connection.openStream(agent.paneId, colsRows.first, colsRows.second) }
    DisposableEffect(agent.paneId) { onDispose { scope.launch { connection.closeStream(agent.paneId) } } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF181825)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Column(Modifier.weight(1f)) { Text(agent.title(), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(agent.agent ?: agent.paneId, color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
            StatusBadge(agent.agentStatus)
            Spacer(Modifier.width(8.dp))
            if (controlling) Button(onClick = { scope.launch { connection.releaseControl(agent.paneId) } }) { Text("Release") }
            else Button(onClick = { confirmTakeover = true }) { Text("Take control") }
        }
        HerdrUnavailableBanner(herdrStatus)
        TerminalPane(
            paneId = agent.paneId,
            frames = store.frames,
            controlling = controlling,
            fontSizeSp = fontSize,
            onInput = { bytes -> if (controlling) scope.launch { connection.inputBytes(agent.paneId, bytes) } },
            onResize = { c, r -> colsRows = c to r; scope.launch { connection.resize(agent.paneId, c, r) } },
            onScrollLines = { lines -> if (controlling) scope.launch { connection.scroll(agent.paneId, if (lines > 0) ScrollDirection.UP else ScrollDirection.DOWN, kotlin.math.abs(lines)) } },
            onFontSizeChanged = { scope.launch { settings.setTerminalFontSize(it) } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        ExtraKeysBar(controlling = controlling, onKey = { spec ->
            scope.launch {
                when (spec) {
                    is KeySpec.Bytes -> if (controlling) connection.inputBytes(agent.paneId, spec.bytes) else connection.sendText(agent.paneId, spec.textFallback)
                    is KeySpec.HerdrKey -> connection.sendKeys(agent.paneId, listOf(spec.key))
                }
            }
        })
    }
    if (confirmTakeover) AlertDialog(onDismissRequest = { confirmTakeover = false }, title = { Text("Take control?") }, text = { Text("Control mode sends raw keyboard input and resizes the real PC pane.") }, confirmButton = { Button(onClick = { confirmTakeover = false; scope.launch { connection.takeControl(agent.paneId, colsRows.first, colsRows.second, takeover = true) } }) { Text("Take control") } }, dismissButton = { TextButton(onClick = { confirmTakeover = false }) { Text("Cancel") } })
}


private sealed interface KeySpec { data class Bytes(val bytes: ByteArray, val textFallback: String) : KeySpec; data class HerdrKey(val key: String) : KeySpec }

@Composable private fun ExtraKeysBar(controlling: Boolean, onKey: (KeySpec) -> Unit) {
    var ctrl by remember { mutableStateOf(false) }
    val keys = listOf("Esc", "Tab", "Ctrl", "Alt", "Up", "Down", "Left", "Right", "Enter", "Ctrl+C", "PgUp", "PgDn", "Home", "End", "/", "|", "-", "1", "2", "y", "n", "Shift+Tab")
    Row(Modifier.fillMaxWidth().background(Color(0xFF11111B)).horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { label ->
            AssistChip(onClick = {
                if (label == "Ctrl") { ctrl = !ctrl; return@AssistChip }
                onKey(mapKey(label, ctrl)); ctrl = false
            }, label = { Text(if (label == "Ctrl" && ctrl) "CTRL*" else label) })
        }
    }
}

private fun mapKey(label: String, ctrl: Boolean): KeySpec {
    if (ctrl && label.length == 1) return KeySpec.HerdrKey("ctrl+${label.lowercase()}")
    return when (label) {
        "Esc" -> KeySpec.HerdrKey("esc")
        "Tab" -> KeySpec.HerdrKey("tab")
        "Alt" -> KeySpec.HerdrKey("alt+x")
        "Up" -> KeySpec.HerdrKey("up")
        "Down" -> KeySpec.HerdrKey("down")
        "Left" -> KeySpec.HerdrKey("left")
        "Right" -> KeySpec.HerdrKey("right")
        "Enter" -> KeySpec.HerdrKey("enter")
        "Ctrl+C" -> KeySpec.HerdrKey("ctrl+c")
        "PgUp" -> KeySpec.HerdrKey("pageup")
        "PgDn" -> KeySpec.HerdrKey("pagedown")
        "Home" -> KeySpec.HerdrKey("home")
        "End" -> KeySpec.HerdrKey("end")
        "Shift+Tab" -> KeySpec.HerdrKey("shift+tab")
        else -> KeySpec.Bytes(label.encodeToByteArray(), label)
    }
}
