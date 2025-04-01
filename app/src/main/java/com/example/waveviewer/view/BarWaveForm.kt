package com.example.waveviewer.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_processing.RMSCompute
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

object BarWaveForm {

    @Composable
    fun BarWaveForm(
        clock: LiveFeedClock,
        audioPlayer: AudioPlayer,
        sampleRate: Int,
        bitDepth: Int,
        bufferSampleSize: Int = sampleRate,
        jumpToPosition: (Float) -> Unit,
        getNextFrame: (sampleCount: Int) -> PCMFrame?
    ) {
        val samplesPerBar = (sampleRate  * ( clock.getDelayTime() / 1000f)).toInt()
        val barCountBufferSize = sampleRate / samplesPerBar
        val currentTick by clock.getClock().collectAsState(0)
        var currentProgress by remember { mutableFloatStateOf(00.0f) }
        val currentFrame = remember { mutableStateListOf<Double>() }
        var reset by remember { mutableStateOf(false) }
        LaunchedEffect(reset) {
            withContext(Dispatchers.IO){
                val frame = getNextFrame(samplesPerBar * barCountBufferSize)?.let {
                    val processed = RMSCompute.computeRMS(it, samplesPerBar)
                    currentFrame.addAll(processed)
                }

            }
        }
       LaunchedEffect(currentTick) {
            withContext(Dispatchers.IO) {

                currentProgress = (currentTick ) / clock.getLengthInMs().toFloat()
                jumpToPosition(currentProgress)
                val frame = getNextFrame(samplesPerBar) ?: return@withContext
                val processed = RMSCompute.computeRMS(frame , samplesPerBar)
                val minCount = min(processed.size , frame.size)
                if(currentFrame.isNotEmpty()){
                    currentFrame.removeRange(0 , minCount)
                }

                currentFrame.addAll(processed)
            }
        }



        Column(
            modifier = Modifier.wrapContentHeight().fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BarList(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), bitDepth =bitDepth,
                bars = currentFrame.toTypedArray()
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

    @Composable
    fun BarList(modifier: Modifier, bars: Array<Double>, bitDepth: Int) {
        val maxAmplitude = (1 shl (bitDepth - 1)).toDouble() *0.5// Max amplitude based on bit depth
        LazyRow(
            modifier = modifier.then(Modifier.fillMaxWidth()),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(bars) { amplitude ->
                BarItem(amplitude / maxAmplitude , modifier = Modifier.fillParentMaxWidth(1f/bars.size))
            }
        }
    }

    @Composable
    fun BarItem(heightRatio: Double , modifier: Modifier) {
        val normalizedHeight = (heightRatio * 100).dp // Scale height proportionally
        Box(
            modifier = modifier.then(Modifier
                .height(normalizedHeight.coerceAtLeast(4.dp)) // Ensure bars are visible
                .background(Color.Cyan)
                ))
    }
}