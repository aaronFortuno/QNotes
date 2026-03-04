package com.aaronfortuno.studio.qnotes.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronfortuno.studio.qnotes.data.model.ItemType
import com.aaronfortuno.studio.qnotes.data.model.VaultItem
import com.aaronfortuno.studio.qnotes.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureUiState(
    val noteText: String = "",
    val clipboardContent: String = "",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun onNoteTextChanged(text: String) {
        _uiState.update { it.copy(noteText = text) }
    }

    fun setClipboardContent(content: String) {
        _uiState.update { it.copy(clipboardContent = content) }
    }

    fun saveQuickNote() {
        val text = _uiState.value.noteText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val item = VaultItem(
                title = text.lines().first().take(50),
                content = text,
                type = ItemType.NOTE
            )
            repository.save(item)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }

    fun saveClipboardContent() {
        val content = _uiState.value.clipboardContent.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val item = VaultItem(
                title = content.lines().first().take(50),
                content = content,
                type = ItemType.CLIPBOARD
            )
            repository.save(item)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }

    fun saveScreenshot(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val item = VaultItem(
                title = "Screenshot",
                content = "",
                type = ItemType.IMAGE
            )
            repository.save(item, imageBytes)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}
