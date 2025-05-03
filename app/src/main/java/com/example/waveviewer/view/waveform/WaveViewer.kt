package com.example.waveviewer.view.waveform

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_processing.RMSCompute
import com.example.waveviewer.audio_stream.pcm.mono.MonoPCMStream
import com.example.waveviewer.audio_stream.pcm.stereo.StereoPCMStream
import kotlin.math.roundToInt

/**
 * A composable that displays a mono audio waveform with navigation controls.
 *
 * @param modifier The modifier to be applied to the composable
 * @param totalFrames The total number of frames to divide the audio into
 * @param visibleFrameCount The number of frames visible at once
 * @param labelStep Controls how often duration labels appear (e.g., 1 = every frame, 2 = every other frame)
 * @param songDurationInMs The total duration of the song in milliseconds
 * @param stream The mono PCM audio stream
 */
@Composable
fun MonoWaveViewer(
    modifier: Modifier,
    totalFrames: Int,
    visibleFrameCount: Int,
    labelStep: Int = visibleFrameCount * 1 / 5,
    songDurationInMs: Int,
    stream: MonoPCMStream
) {
    // Ensure stream is open
    if (!stream.isOpen()) {
        stream.open()
    }

    // Calculate frame length based on total duration and number of frames
    val frameLengthMs = songDurationInMs / totalFrames.coerceAtLeast(1)

    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    val currentFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }

    // Calculate samples per frame using floating point division
    val samplesPerFrame = (frameLengthMs.toFloat() / 1000f) * stream.getDescriptor().getSampleRate()

    // Calculate an appropriate number of bars per frame (10% of samples is a reasonable starting point)
    val samplesPerBar = (samplesPerFrame * 0.1).roundToInt().coerceAtLeast(1)

    // Determine if scrolling should be enabled
    val isScrollable = totalFrames > visibleFrameCount

    Log.d(
        "TEST",
        "Song Length: ${formatDuration(songDurationInMs.toFloat())}, " +
                "Frame Length: ${frameLengthMs}ms, " +
                "Samples Per Frame: $samplesPerFrame, " +
                "Samples Per Bar: $samplesPerBar"
    )

    LaunchedEffect(currentProgress) {
        Log.d("TEST", "Current Progress: $currentProgress")
        currentFrames.clear()

        // Calculate starting frame index
        val startFrameIndex = ((currentProgress * (totalFrames - visibleFrameCount))).roundToInt()
        var currentTimeStampInMs = startFrameIndex * frameLengthMs

        // Set stream progress
        stream.setProgress(currentTimeStampInMs / songDurationInMs.toFloat())

        // Read frames
        repeat(visibleFrameCount) { frameIndex ->
            val exactSampleCount = samplesPerFrame.roundToInt()
            if (exactSampleCount <= 0) {
                Log.e("TEST", "Error: Calculated sample count is $exactSampleCount. Using minimum of 1.")
                return@LaunchedEffect
            }

            val currentFrame = stream.readNextFrame(exactSampleCount) ?: return@LaunchedEffect

            // Accumulate the duration
            currentTimeStampInMs += frameLengthMs

            // Generate label based on the step
            val showLabel = (frameIndex % labelStep) == 0
            val label = if (showLabel) formatDuration(currentTimeStampInMs.toFloat()) else ""

            // Perform down sampling
            val downSampledFrame = RMSCompute.computeRMS(currentFrame, samplesPerBar)

            Log.d("TEST", "Frame: $frameIndex, Timestamp: $currentTimeStampInMs, Label: $label, RMS Count: ${downSampledFrame.size}")

            // Append new data
            val newDataFrame = Pair(downSampledFrame, label)
            currentFrames.add(newDataFrame)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaveForm(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            padding = PaddingValues(10.dp),
            currentFrames
        )

        // Show progress bar only if scrollable
        AnimatedVisibility(isScrollable) {
            ProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                currentProgress = { currentProgress }
            ) {
                currentProgress = it
            }
        }
    }
}
@Composable
fun StereoWaveViewer(
    modifier: Modifier,
    totalFrames: Int,
    visibleFrameCount: Int,
    labelStep: Int = visibleFrameCount * 1 / 5,
    songDurationInMs: Int,
    stream: StereoPCMStream
) {
    // Ensure stream is open
    if (!stream.isOpen()) {
        stream.open()
    }

    // Calculate frame length based on total duration and number of frames
    val frameLengthMs = songDurationInMs / totalFrames.coerceAtLeast(1)

    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    val leftFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val rightFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    // Calculate samples per frame using floating point division
    val samplesPerFrame = (frameLengthMs.toFloat() / 1000f) * stream.getDescriptor().getSampleRate()

    // Calculate an appropriate number of bars per frame (10% of samples is a reasonable starting point)
    val samplesPerBar = (samplesPerFrame * 0.1).roundToInt().coerceAtLeast(1)

    // Determine if scrolling should be enabled
    val isScrollable = totalFrames > visibleFrameCount

    Log.d(
        "TEST",
        "Song Length: ${formatDuration(songDurationInMs.toFloat())}, " +
                "Frame Length: ${frameLengthMs}ms, " +
                "Samples Per Frame: $samplesPerFrame, " +
                "Samples Per Bar: $samplesPerBar"
    )

    LaunchedEffect(currentProgress) {
        Log.d("TEST", "Current Progress: $currentProgress")
        leftFrames.clear()
        rightFrames.clear()

        // Calculate starting frame index
        val startFrameIndex = ((currentProgress * (totalFrames - visibleFrameCount))).roundToInt()
        var currentTimeStampInMs = startFrameIndex * frameLengthMs

        // Set stream progress
        stream.setProgress(currentTimeStampInMs / songDurationInMs.toFloat())

        // Read frames
        repeat(visibleFrameCount) { frameIndex ->
            val exactSampleCount = samplesPerFrame.roundToInt()
            if (exactSampleCount <= 0) {
                Log.e("TEST", "Error: Calculated sample count is $exactSampleCount. Using minimum of 1.")
                return@LaunchedEffect
            }

            val currentFrame = stream.readNextFrame(exactSampleCount) ?: return@LaunchedEffect

            // Accumulate the duration
            currentTimeStampInMs += frameLengthMs

            // Generate label based on the step
            val showLabel = (frameIndex % labelStep) == 0
            val label = if (showLabel) formatDuration(currentTimeStampInMs.toFloat()) else ""

            // Perform down sampling

            val leftChannel = RMSCompute.computeRMS(currentFrame.leftChannel)
            val rightChannel = RMSCompute.computeRMS(currentFrame.rightChannel)
            Log.d("TEST", "Frame: $frameIndex, Timestamp: $currentTimeStampInMs, Label: $label, RMS Count: ${leftChannel.size}")

            // Append new data
            val left = Pair(leftChannel, label)
            val right = Pair(rightChannel, label)
            leftFrames.add(left)
            rightFrames.add(right)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaveForm(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            padding = PaddingValues(10.dp),
            leftFrames
        )
        WaveForm(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            padding = PaddingValues(10.dp),
            rightFrames
        )
        // Show progress bar only if scrollable
        AnimatedVisibility(isScrollable) {
            ProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                currentProgress = { currentProgress }
            ) {
                currentProgress = it
            }
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