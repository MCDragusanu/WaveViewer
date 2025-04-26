package com.example.waveviewer.view.waveform

import android.util.Log
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
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMInputStream

@Composable
fun WaveViewer(modifier : Modifier, windowSize : Int, songDurationInMs : Int,samplesPerFrame : Int = 44100, downSampleFactor : Int = 10, stream : PCMInputStream) {

    if (!stream.isOpen()) {
        stream.open()
    }

    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    val currentFrames = remember { mutableStateListOf<Pair<Array<Double>, String>>() }
    val samplesPerBar = 441
    Log.d("TEST" , "Song Length : ${formatDuration(songDurationInMs.toFloat())} , SamplesPerBar : $samplesPerBar")

    LaunchedEffect(currentProgress) {

        Log.d("TEST" , "Current Progress : $currentProgress")
        currentFrames.clear()
        //obtain the starting timestamp
        var firstTimeStampInMs = currentProgress * songDurationInMs

        stream.setProgress(currentProgress)

        repeat(windowSize) {
            //obtain the next frame
            val currentFrame = stream.readNextFrame(samplesPerFrame) ?: return@LaunchedEffect

            //compute the duration in ms of the frame
            val frameSampleCount = currentFrame.getBytes().size / currentFrame.getSampleByteStride()
            val frameDurationMs =
                frameSampleCount * 1.0f / stream.getDescriptor().getSampleRate() * 1000f

            //accumulate the duration
            firstTimeStampInMs += frameDurationMs

            //generate new label
            val label = formatDuration(firstTimeStampInMs)

            //Perform down sampling
            val downSampledFrame =
                RMSCompute.computeRMS(currentFrame, samplesPerBar)

            Log.d("TEST" , "Frame Timestamp : ${label} RMS Count : ${downSampledFrame.size}")

            //append new data
            val newDataFrame = Pair(downSampledFrame, label)
            currentFrames.add(newDataFrame)
        }
    }
    Column(
        modifier =modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaveForm(modifier = Modifier.fillMaxWidth().wrapContentHeight() , padding = PaddingValues(10.dp) , currentFrames)
        ProgressBar(modifier = Modifier.fillMaxWidth().height(40.dp) , currentProgress = {currentProgress}) {
            currentProgress = it
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