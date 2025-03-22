package com.example.waveviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


interface AudioPlayer {
    @Composable
    fun Content(
        modifier: Modifier,
        currentProgress: Float,
        totalDurationMs: Long,
        onPause: () -> Unit,
        onStart: () -> Unit,
        onPositionChanged: (Float) -> Unit
    )
}