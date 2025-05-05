package com.example.waveviewer.view.waveform

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_processing.RMSCompute
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

/**
 * A wave viewer that can display waveforms for mono or stereo audio streams.
 * Progress is controlled externally through a Flow of Float values.
 *
 * @param modifier The modifier to be applied to the component
 * @param totalFrames Total number of frames in the audio
 * @param visibleFrameCount Number of frames visible at once
 * @param songDurationInMs Duration of the song in milliseconds
 * @param stream The PCM stream wrapper (mono or stereo)
 * @param progressFlow Flow of progress values (0.0f to 1.0f)
 * @param onProgressChanged Callback when progress is changed by user interaction with the progress bar
 */
@Composable
fun UniversalWaveViewer(
    modifier: Modifier,
    totalFrames: Int,
    visibleFrameCount: Int,
    songDurationInMs: Int,
    stream: PCMStreamWrapper, // MonoPCMStream or StereoPCMStream
    progressFlow: Flow<Float> = flowOf(0f), // External progress flow with default value
) {
    val isMono = stream is PCMStreamWrapper.Mono
    val isStereo = stream is PCMStreamWrapper.Stereo

    // Collect the current progress from the flow
    val progress by progressFlow.collectAsState(initial = 0f)


    val frameLengthMs = songDurationInMs / totalFrames.coerceAtLeast(1)
    val sampleRate = stream.getDescriptor().getSampleRate()

    val monoFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val leftFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val rightFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }

    val samplesPerFrame = (frameLengthMs.toFloat() / 1000f) * sampleRate
    val samplesPerBar = (samplesPerFrame * 0.10).roundToInt().coerceAtLeast(1)


    LaunchedEffect(Unit) {
        if (!stream.isOpen()) {
            stream.open()
        }
    }


    LaunchedEffect(progress) {
        val startFrameIndex = ((progress * (totalFrames - visibleFrameCount))).roundToInt()
        var currentTimeStampInMs = startFrameIndex * frameLengthMs

        val ratio = currentTimeStampInMs.toFloat() / songDurationInMs

        stream.setProgress(ratio)

        monoFrames.clear()
        leftFrames.clear()
        rightFrames.clear()

        repeat(visibleFrameCount) { frameIndex ->
            val exactSampleCount = samplesPerFrame.roundToInt()
            if (exactSampleCount <= 0) return@LaunchedEffect

            if (isMono) {
                val currentFrame = (stream as PCMStreamWrapper.Mono).readNextFrame(exactSampleCount) ?: return@LaunchedEffect
                val downSampled = RMSCompute.computeRMS(currentFrame, samplesPerBar)
                monoFrames.add(downSampled to "")
                Log.d("WaveViewer", "Fetched ${currentFrame.size} mono channel samples")
            } else if (isStereo) {
                val currentFrame =
                    (stream as PCMStreamWrapper.Stereo).readNextFrame(exactSampleCount)
                        ?: return@LaunchedEffect
                val left = RMSCompute.computeRMS(currentFrame.leftChannel)
                val right = RMSCompute.computeRMS(currentFrame.rightChannel)
                leftFrames.add(left to "")
                rightFrames.add(right to "")
                Log.d("WaveViewer", "Fetched ${currentFrame.leftChannel.size} left/right channel samples")
            }

            currentTimeStampInMs += frameLengthMs
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isMono) {
            // Channel label with proper styling
            Text(
                text = "Mono Channel",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight() , contentAlignment = Alignment.CenterStart) {
                WaveForm(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    padding = PaddingValues(10.dp),
                    bitDepth = stream.getDescriptor().getBitDepth(),
                    frameData = monoFrames
                )
                // Pass the cursor bounds to prevent overflow
                CursorOverlay(
                    progress = progress,
                    // Cap progress to prevent cursor from going beyond visible area
                    constrainEndBounds = true
                )
            }
        } else if (isStereo) {
            // Left channel label with proper styling
            Text(
                text = "Left Channel",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentAlignment = Alignment.CenterStart) {
                WaveForm(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    padding = PaddingValues(0.dp),
                    bitDepth = stream.getDescriptor().getBitDepth(),
                    frameData = leftFrames
                )
                CursorOverlay(
                    progress = progress,
                    constrainEndBounds = true
                )
            }

            // Right channel label with proper styling
            Text(
                text = "Right Channel",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight() ,  contentAlignment = Alignment.CenterStart) {
                WaveForm(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    padding = PaddingValues(0.dp),
                    bitDepth = stream.getDescriptor().getBitDepth(),
                    frameData = rightFrames
                )
                CursorOverlay(
                    progress = progress,
                    constrainEndBounds = true
                )
            }
        }
    }
}

@Composable
fun CursorOverlay(
    progress: Float,
    constrainEndBounds: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 4.dp)
    ) {
        val lineXOffset = with(LocalDensity.current) {
            // Get the available width for the cursor
            val widthInPx = LocalConfiguration.current.screenWidthDp.dp.toPx()
            val paddingPx = 4.dp.toPx() * 2 // Account for horizontal padding
            val availableWidth = widthInPx - paddingPx

            // Calculate cursor position
            val cursorPosition = if (constrainEndBounds) {
                // If constraining bounds, ensure cursor stays within the available width
                // Leave 1dp space at the end to keep cursor visible
                (availableWidth * progress).coerceIn(0f, availableWidth - 1.dp.toPx())
            } else {
                // Original behavior
                (availableWidth * progress).coerceIn(0f, availableWidth)
            }

            // Add left padding to the position
            cursorPosition + 4.dp.toPx()
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.Red,
                start = Offset(x = lineXOffset, y = 0f),
                end = Offset(x = lineXOffset, y = size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
fun formatDuration(durationInMs: Float): String {
    val totalSeconds = (durationInMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (durationInMs % 1000).toInt()

    return if (minutes > 0) {
        // e.g. "1:04.200" -> "1:04.2"  or "2:00.000" -> "2:00"
        var text = String.format("%d:%02d.%03d", minutes, seconds, milliseconds)
        // remove trailing zeros after the dot
        text = text.replace(Regex("(\\.\\d*?)0+$"), "$1")
        // if it ends in a '.', drop it too
        text = text.replace(Regex("\\.$"), "")
        text
    } else {
        // e.g. "5.400s" -> "5.4s"  or "3.000s" -> "3s"
        var text = String.format("%d.%03ds", seconds, milliseconds)
        // strip zeros before the 's'
        text = text.replace(Regex("(\\.\\d*?)0+(?=s$)"), "$1")
        // drop a lone '.', if present
        text = text.replace(Regex("\\.(?=s$)"), "")
        text
    }
}