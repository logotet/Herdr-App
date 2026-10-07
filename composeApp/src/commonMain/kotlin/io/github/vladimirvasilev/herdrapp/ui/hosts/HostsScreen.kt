package io.github.vladimirvasilev.herdrapp.ui.hosts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.data.PairUriParser
import io.github.vladimirvasilev.herdrapp.data.SavedHost
import io.github.vladimirvasilev.herdrapp.ui.LocalQrScannerService

@Composable
internal fun HostsScreen(
    hostList: List<SavedHost>,
    onBack: () -> Unit,
    onSave: (SavedHost) -> Unit,
    onSelect: (SavedHost) -> Unit,
    onDelete: (SavedHost) -> Unit,
) {
    val scanner = LocalQrScannerService.current
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hosts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Done") }
            }
        }
        items(hostList, key = { it.id }) { saved ->
            SavedHostRow(saved, onSelect = { onSelect(saved) }, onDelete = { onDelete(saved) })
        }
        item { Divider() }
        item {
            Button(
                onClick = {
                    scanner.scan { raw ->
                        raw?.let { PairUriParser.parse(it)?.let(onSave) ?: run { error = "Invalid QR" } }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Scan pairing QR") }
        }
        item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(token, { token = it }, label = { Text("Token") }, modifier = Modifier.fillMaxWidth()) }
        item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 8787
                    if (host.isBlank() || token.isBlank()) {
                        error = "Host and token are required"
                    } else {
                        onSave(
                            SavedHost(
                                id = "manual-${host.hashCode()}-$p",
                                name = name.ifBlank { host },
                                host = host,
                                port = p,
                                token = token,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save host") }
        }
    }
}

@Composable
private fun SavedHostRow(saved: SavedHost, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onSelect() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(saved.name, fontWeight = FontWeight.Bold)
                Text("${saved.host}:${saved.port}", color = Color.Gray)
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
