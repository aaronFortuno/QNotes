package com.aaronfortuno.studio.qnotes.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aaronfortuno.studio.qnotes.R

@Composable
fun QuickCaptureContent(
    uiState: CaptureUiState,
    onNoteTextChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
    onSaveClipboard: () -> Unit,
    onScreenshot: () -> Unit,
    onRequestClipboard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screen by remember { mutableStateOf("menu") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (screen) {
                "menu" -> MenuScreen(
                    onScreenshot = onScreenshot,
                    onQuickNote = { screen = "note" },
                    onClipboard = {
                        onRequestClipboard()
                        screen = "clipboard"
                    }
                )
                "note" -> NoteScreen(
                    noteText = uiState.noteText,
                    isSaving = uiState.isSaving,
                    onTextChanged = onNoteTextChanged,
                    onSave = onSaveNote,
                    onCancel = { screen = "menu" }
                )
                "clipboard" -> ClipboardScreen(
                    clipboardContent = uiState.clipboardContent,
                    isSaving = uiState.isSaving,
                    onSave = onSaveClipboard,
                    onCancel = { screen = "menu" }
                )
            }
        }
    }
}

@Composable
private fun MenuScreen(
    onScreenshot: () -> Unit,
    onQuickNote: () -> Unit,
    onClipboard: () -> Unit
) {
    Text(
        text = "Quick Capture",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(20.dp))

    MenuButton(
        iconRes = R.drawable.ic_screenshot,
        label = "Screenshot",
        onClick = onScreenshot
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuButton(
        iconRes = R.drawable.ic_note_quick,
        label = "Quick Note",
        onClick = onQuickNote
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuButton(
        iconRes = R.drawable.ic_clipboard,
        label = "From Clipboard",
        onClick = onClipboard
    )
}

@Composable
private fun MenuButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NoteScreen(
    noteText: String,
    isSaving: Boolean,
    onTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = "Quick Note",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = noteText,
        onValueChange = onTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        placeholder = { Text("Type your note…") },
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))

    ActionButtons(
        isSaving = isSaving,
        saveEnabled = noteText.isNotBlank(),
        onSave = onSave,
        onCancel = onCancel
    )
}

@Composable
private fun ClipboardScreen(
    clipboardContent: String,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = "From Clipboard",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = clipboardContent.ifEmpty { "(empty clipboard)" },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis
        )
    }
    Spacer(modifier = Modifier.height(16.dp))

    ActionButtons(
        isSaving = isSaving,
        saveEnabled = clipboardContent.isNotBlank(),
        onSave = onSave,
        onCancel = onCancel
    )
}

@Composable
private fun ActionButtons(
    isSaving: Boolean,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
        OutlinedButton(onClick = onCancel, enabled = !isSaving) {
            Text("Cancel")
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save")
            }
        }
    }
}
