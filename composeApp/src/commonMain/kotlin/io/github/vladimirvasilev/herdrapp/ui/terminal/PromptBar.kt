package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Native prompt box: regular keyboard features (autocorrect, swipe, voice, paste). Enter sends;
 * Shift+Enter (hardware) or a long-press on Send inserts a new line; pasted newlines are kept.
 * Sending with an empty box just presses Enter in the pane.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PromptBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
) {
    fun insertNewline() {
        val start = minOf(value.selection.start, value.selection.end)
        val end = maxOf(value.selection.start, value.selection.end)
        val text = value.text.replaceRange(start, end, "\n")
        onValueChange(TextFieldValue(text, TextRange(start + 1)))
    }
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF181825)).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { new -> if (isEnterPress(value.text, new.text)) onSend() else onValueChange(new) },
            modifier = Modifier.weight(1f).onPreviewKeyEvent { e ->
                if (e.key != Key.Enter && e.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                if (e.type == KeyEventType.KeyDown) {
                    if (e.isShiftPressed) insertNewline() else onSend()
                }
                true
            },
            placeholder = { Text("Message agent…") },
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(48.dp)
                .background(if (sending) Color.Gray else MaterialTheme.colorScheme.primary, CircleShape)
                .combinedClickable(enabled = !sending, onClick = onSend, onLongClick = { insertNewline() }),
            contentAlignment = Alignment.Center,
        ) {
            Text("➤", color = Color.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}
