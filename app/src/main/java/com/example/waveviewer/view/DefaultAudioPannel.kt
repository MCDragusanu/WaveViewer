package com.example.waveviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.LinearProgressIndicator as LinearProgressIndicator1

object DefaultAudioPannel : AudioPlayer {
    @Composable
    override fun Content(
        modifier: Modifier,
        currentProgress: Float,
        totalDurationMs: Long,
        onPause: () -> Unit,
        onStart: () -> Unit,
        onPositionChanged: (Float) -> Unit
    ) {
        var isPlaying by remember { mutableStateOf(false) }
        val progress = currentProgress.coerceIn(0f, 1f)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp) .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset->
                                tryAwaitRelease()
                                val normalizedX = offset.x / size.width.toFloat()
                                onPositionChanged(normalizedX.coerceIn(0f, 1f))
                            }
                        )
                    },

                color = Color.Blue,
                trackColor = Color.Blue.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Play / Pause Button
            Button(
                onClick = {
                    isPlaying = !isPlaying
                    if (isPlaying) onStart() else onPause()
                }
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            // Time Display
            Text(text = "Time: ${(progress * totalDurationMs / 1000).toInt()} sec")
        }
    }
}
