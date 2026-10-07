package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Shows [dialog]; [onConfirm] gets the typed name, empty for the dialogs that ask for none. */
@Composable
internal fun LayoutDialog(dialog: HomeDialog, onConfirm: (name: String) -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        is HomeDialog.RenamePane -> NameDialog(
            title = stringResource(Res.string.home_rename_pane_title),
            initial = dialog.current,
            confirm = stringResource(Res.string.home_rename),
            nameRequired = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is HomeDialog.RenameTab -> NameDialog(
            title = stringResource(Res.string.home_rename_tab_title),
            initial = dialog.current,
            confirm = stringResource(Res.string.home_rename),
            nameRequired = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is HomeDialog.NewTab -> NameDialog(
            title = stringResource(Res.string.home_new_tab_title, dialog.workspaceLabel),
            initial = "",
            confirm = stringResource(Res.string.home_create),
            nameRequired = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        HomeDialog.NewWorkspace -> NameDialog(
            title = stringResource(Res.string.home_new_workspace_title),
            initial = "",
            confirm = stringResource(Res.string.home_create),
            nameRequired = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is HomeDialog.RenameWorkspace -> NameDialog(
            title = stringResource(Res.string.home_rename_workspace_title),
            initial = dialog.current,
            confirm = stringResource(Res.string.home_rename),
            nameRequired = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is HomeDialog.CloseWorkspace -> CloseDialog(
            title = stringResource(Res.string.home_close_workspace_title, dialog.label),
            body = stringResource(Res.string.home_close_workspace_body, dialog.paneCount),
            onConfirm = { onConfirm("") },
            onDismiss = onDismiss,
        )
        is HomeDialog.ClosePane -> CloseDialog(
            title = stringResource(Res.string.home_close_pane_title, dialog.title),
            body = stringResource(Res.string.home_close_pane_body),
            onConfirm = { onConfirm("") },
            onDismiss = onDismiss,
        )
        is HomeDialog.CloseTab -> CloseDialog(
            title = stringResource(Res.string.home_close_tab_title, dialog.label),
            body = stringResource(Res.string.home_close_tab_body),
            onConfirm = { onConfirm("") },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    nameRequired: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // The current name starts out selected, so typing replaces it.
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.home_name_label)) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.text) }, enabled = !nameRequired || name.text.isNotBlank()) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.home_cancel)) } },
    )
}

@Composable
private fun CloseDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(Res.string.home_close))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.home_cancel)) } },
    )
}
