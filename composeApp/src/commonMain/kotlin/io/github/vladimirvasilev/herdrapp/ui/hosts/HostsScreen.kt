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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.domain.SavedHost

@Composable
internal fun HostsRoute(container: AppContainer, onDone: () -> Unit) {
    val viewModel = viewModel { HostsViewModel(container.hosts, container.session) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scan = LocalQrScanner.current.rememberLauncher { raw -> if (viewModel.onScanned(raw)) onDone() }
    HostsScreen(
        state = state,
        onBack = onDone,
        onSelect = { host ->
            viewModel.select(host)
            onDone()
        },
        onDelete = viewModel::delete,
        onScan = scan,
        onNameChange = viewModel::onNameChange,
        onHostChange = viewModel::onHostChange,
        onPortChange = viewModel::onPortChange,
        onTokenChange = viewModel::onTokenChange,
        onSave = { if (viewModel.saveForm()) onDone() },
    )
}

@Composable
internal fun HostsScreen(
    state: HostsUiState,
    onBack: () -> Unit,
    onSelect: (SavedHost) -> Unit,
    onDelete: (SavedHost) -> Unit,
    onScan: () -> Unit,
    onNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val form = state.form
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hosts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Done") }
            }
        }
        items(state.hosts, key = { it.id }) { saved ->
            SavedHostRow(saved, onSelect = { onSelect(saved) }, onDelete = { onDelete(saved) })
        }
        item { Divider() }
        item { Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan pairing QR") } }
        item { FormField(form.name, onNameChange, "Name") }
        item { FormField(form.host, onHostChange, "Host") }
        item { FormField(form.port, onPortChange, "Port") }
        item { FormField(form.token, onTokenChange, "Token") }
        item { form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save host") } }
    }
}

@Composable
private fun FormField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
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
