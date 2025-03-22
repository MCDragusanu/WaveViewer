package com.example.waveviewer.view

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow

object WaveForm {


    @Composable
    fun WaveFormElement(modifier: Modifier, frame: Collection<PCMSample>, bitDepth: Int) {


        val samples = frame.toList()
        if (samples.isEmpty()) return
        val sampleCount = samples.size
        val pcmMax = 2.0.pow(bitDepth) - 1

        Canvas(modifier) {
            val width = this.size.width
            val height = this.size.width


            val maxAmplitude = (height / 2.0f)
            val amplitudeScaleFactor = maxAmplitude / pcmMax
            val sampleDistance = width / (sampleCount - 1).toFloat()

            var prevX = 0f
            var prevY = (height / 2.0f - samples.first().getValue() * amplitudeScaleFactor)



            for (i in 1 until sampleCount) {
                val current = samples[i].getValue()
                // Log.d("Test" , current.toString())
                val x = prevX + sampleDistance

                val y = height / 2.0f - current * amplitudeScaleFactor
                drawLine(
                    color = Color.Cyan,
                    start = Offset(prevX, prevY.toFloat()),
                    end = Offset(x, y.toFloat()),
                    strokeWidth = 4f
                )

                prevX = x
                prevY = y
            }
        }
    }

    @Composable
    fun LiveFeed(
        clock: LiveFeedClock,
        audioPlayer: AudioPlayer,
        sampleRate: Int,
        bitDepth: Int,
        bufferSampleSize : Int = sampleRate /8,
        jumpToPosition: (Float) -> Unit,
        getNextFrame: (sampleCount: Int) -> PCMFrame?

    ) {
        val samplesToShift = (sampleRate * (clock.getDelayTime() / 1000f)).toInt()
        val currentTick by clock.getClock().collectAsState(0)
        var currentProgress by remember { mutableFloatStateOf(00.0f) }
        val currentFrame = remember { mutableStateListOf<PCMSample>() }
        var reset by remember { mutableStateOf(false) }
        LaunchedEffect(reset) {
            withContext(Dispatchers.IO) {
                currentFrame.clear()
                val frame = getNextFrame(bufferSampleSize) ?: return@withContext
                currentFrame.addAll(frame)
            }
        }

        LaunchedEffect(currentTick) {
            withContext(Dispatchers.IO) {
                currentProgress = currentTick / clock.getLengthInMs().toFloat()
                jumpToPosition(currentProgress)
                val frame = getNextFrame(samplesToShift) ?: return@withContext
                val removeCount = min(samplesToShift, currentFrame.size)
                if (removeCount > 0) {
                    currentFrame.subList(0, removeCount).clear()
                }
                Log.d(
                    "Test ",
                    "Current Tick : ${currentTick / 1000} s FrameSize : ${currentFrame.size} Progress : ${currentProgress} TotalLength :${clock.getLengthInMs() / 1000}s"
                )
                currentFrame.addAll(frame)
            }
        }



        Column(
            modifier = Modifier.wrapContentHeight().fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaveFormElement(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                frame = currentFrame,
                bitDepth = bitDepth
            )
            audioPlayer.Content(
                modifier = Modifier.fillMaxWidth(),
                currentProgress = currentProgress,
                totalDurationMs = clock.getLengthInMs(),
                onPause = {
                    clock.pause()
                },
                onStart = {
                    clock.start()
                },
                onPositionChanged = { progress ->
                    reset = !reset
                    clock.setProgress(progress)
                    jumpToPosition(progress)
                })

        }
    }
}

