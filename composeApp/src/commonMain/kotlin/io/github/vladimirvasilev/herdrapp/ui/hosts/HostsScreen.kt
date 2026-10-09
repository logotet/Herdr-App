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
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.domain.SavedHost

@Composable
internal fun HostsRoute(container: AppContainer, onDone: () -> Unit) {
    val viewModel = viewModel { HostsViewModel(container.hosts, container.session, container.settings) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val askToNotify = LocalNotificationPermission.current.rememberRequest { granted ->
        viewModel.setBackgroundAlerts(enabled = true, mayNotify = granted)
    }
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
        onBackgroundAlerts = { enabled -> if (enabled) askToNotify() else viewModel.setBackgroundAlerts(false) },
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
    onBackgroundAlerts: (Boolean) -> Unit,
) {
    val form = state.form
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.hosts_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text(stringResource(Res.string.hosts_done)) }
            }
        }
        items(state.hosts, key = { it.id }) { saved ->
            SavedHostRow(saved, onSelect = { onSelect(saved) }, onDelete = { onDelete(saved) })
        }
        item { Divider() }
        item { BackgroundAlertsRow(state.backgroundAlerts, state.alertsDenied, onBackgroundAlerts) }
        item { Divider() }
        item { Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.hosts_scan)) } }
        item { FormField(form.name, onNameChange, stringResource(Res.string.hosts_field_name)) }
        item { FormField(form.host, onHostChange, stringResource(Res.string.hosts_field_host)) }
        item { FormField(form.port, onPortChange, stringResource(Res.string.hosts_field_port)) }
        item { FormField(form.token, onTokenChange, stringResource(Res.string.hosts_field_token)) }
        item { form.error?.let { Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.hosts_save)) } }
    }
}

private fun HostFormError.message(): StringResource = when (this) {
    HostFormError.MISSING_FIELDS -> Res.string.hosts_error_missing_fields
    HostFormError.INVALID_QR -> Res.string.hosts_error_invalid_qr
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
                Text("${saved.host}:${saved.port}", color = HerdrTheme.colors.muted)
            }
            TextButton(onClick = onDelete) { Text(stringResource(Res.string.hosts_delete)) }
        }
    }
}
