package com.aaronfortuno.studio.qnotes.ui.capture

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aaronfortuno.studio.qnotes.service.ScreenshotService
import com.aaronfortuno.studio.qnotes.ui.theme.QNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickCaptureActivity : ComponentActivity() {

    private val viewModel: CaptureViewModel by viewModels()

    private val screenshotLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenshotService::class.java).apply {
                putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenshotService.EXTRA_DATA, result.data)
            }
            startForegroundService(intent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QNotesTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.savedSuccessfully) {
                    if (uiState.savedSuccessfully) finish()
                }

                QuickCaptureContent(
                    uiState = uiState,
                    onNoteTextChanged = viewModel::onNoteTextChanged,
                    onSaveNote = viewModel::saveQuickNote,
                    onSaveClipboard = viewModel::saveClipboardContent,
                    onScreenshot = ::requestScreenshot,
                    onRequestClipboard = ::loadClipboard,
                    onDismiss = ::finish
                )
            }
        }
    }

    private fun loadClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(this).toString()
                    viewModel.setClipboardContent(text)
                }
            }
        } catch (_: SecurityException) {
            // Android 13+ may restrict clipboard access
        }
    }

    private fun requestScreenshot() {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenshotLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
