package com.example.waveviewer.view.waveform

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
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamWrapper
import kotlin.math.roundToInt



@Composable
fun UniversalWaveViewer(
    modifier: Modifier,
    totalFrames: Int,
    visibleFrameCount: Int,
    songDurationInMs: Int,
    stream: PCMStreamWrapper // MonoPCMStream or StereoPCMStream
) {
    val isMono = stream is PCMStreamWrapper.Mono
    val isStereo = stream is PCMStreamWrapper.Stereo

    val labelStep = visibleFrameCount.coerceAtLeast(1) / 5
    val frameLengthMs = songDurationInMs / totalFrames.coerceAtLeast(1)
    val sampleRate = stream.getDescriptor().getSampleRate()

    var currentProgress by remember { mutableFloatStateOf(0.0f) }

    val monoFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val leftFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val rightFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }

    val samplesPerFrame = (frameLengthMs.toFloat() / 1000f) * sampleRate
    val samplesPerBar = (samplesPerFrame * 0.10).roundToInt().coerceAtLeast(1)
    val isScrollable = totalFrames > visibleFrameCount

    // Ensure the stream is open
    LaunchedEffect(Unit) {
      if(!stream.isOpen()){
          stream.open()
      }
    }

    LaunchedEffect(currentProgress) {
        val startFrameIndex = ((currentProgress * (totalFrames - visibleFrameCount))).roundToInt()
        var currentTimeStampInMs = startFrameIndex * frameLengthMs

        val ratio = currentTimeStampInMs.toFloat() / songDurationInMs
        stream.setProgress(ratio)

        monoFrames.clear()
        leftFrames.clear()
        rightFrames.clear()

        repeat(visibleFrameCount) { frameIndex ->
            val exactSampleCount = samplesPerFrame.roundToInt()
            if (exactSampleCount <= 0) return@LaunchedEffect

            val label = if (frameIndex % labelStep == 0) formatDuration(currentTimeStampInMs.toFloat()) else ""

            if (isMono) {
                val currentFrame = (stream as PCMStreamWrapper.Mono).readNextFrame(exactSampleCount) ?: return@LaunchedEffect
                val downSampled = RMSCompute.computeRMS(currentFrame, samplesPerBar)
                monoFrames.add(downSampled to label)
            } else if (isStereo) {
                val currentFrame = (stream as PCMStreamWrapper.Stereo).readNextFrame(exactSampleCount) ?: return@LaunchedEffect
                val left = RMSCompute.computeRMS(currentFrame.leftChannel)
                val right = RMSCompute.computeRMS(currentFrame.rightChannel)
                leftFrames.add(left to label)
                rightFrames.add(right to label)
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
            WaveForm(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                padding = PaddingValues(10.dp),
                bitDepth = stream.getDescriptor().getBitDepth(),
                frameData = monoFrames
            )
        } else if (isStereo) {
            WaveForm(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                padding = PaddingValues(0.dp),
                bitDepth = stream.getDescriptor().getBitDepth(),
                frameData = leftFrames
            )
            WaveForm(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                padding = PaddingValues(0.dp),
                bitDepth = stream.getDescriptor().getBitDepth(),
                frameData = rightFrames
            )
        }

        AnimatedVisibility(isScrollable) {
            ProgressBar(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                currentProgress = { currentProgress },
                onProgressChanged = { currentProgress = it }
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