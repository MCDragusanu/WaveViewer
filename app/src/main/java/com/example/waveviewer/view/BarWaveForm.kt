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
    fun LiveFeedBarWaveform(
        clock: LiveFeedClock,
        audioPlayer: AudioPlayer,
        sampleRate: Int,
        bitDepth: Int,
        bufferSampleSize: Int = sampleRate,
        jumpToPosition: (Float) -> Unit,
        getNextFrame: (sampleCount: Int) -> PCMFrame?
    ) {
        val samplesPerBar = (sampleRate  * ( clock.getDelayTime() / 1000f)).toInt()
        val barCountBufferSize = 100//sampleRate / samplesPerBar
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

                val frame = getNextFrame(samplesPerBar) ?: return@withContext
                val processed = RMSCompute.computeRMS(frame , samplesPerBar)
                val minCount = min(processed.size , frame.size)
                if(currentFrame.isNotEmpty()){
                    currentFrame.removeRange(0 , minCount)
                }
                currentFrame.addAll(processed)
                val latencyTime= (samplesPerBar * 2 / sampleRate) * 1000f
                currentProgress = (currentTick + latencyTime) / clock.getLengthInMs().toFloat()
                jumpToPosition(currentProgress)

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

    //Sa zic ca impart in N diviziuni
    //Calculez lungimea in timp  fiecarei divizuni
    @Composable
    fun StaticWaveBar(
        clock: LiveFeedClock,
        audioPlayer: AudioPlayer,
        sampleRate: Int,
        bitDepth: Int,
        jumpToPosition: (Float) -> Unit,
        getNextFrame: (sampleCount: Int) -> PCMFrame?
    ) {
        val samplesPerBar = sampleRate / 10
        val barCountBufferSize = 10 //sampleRate / samplesPerBar
        val currentTick by clock.getClock().collectAsState(0)
        var currentProgress by remember { mutableFloatStateOf(00.0f) }
        val currentFrame = remember { mutableStateListOf<Double>() }
        var reset by remember { mutableStateOf(false) }
        LaunchedEffect(true) {
            withContext(Dispatchers.IO) {
                var batch: PCMFrame? = null
                do {
                    batch = getNextFrame(samplesPerBar * barCountBufferSize)
                    batch?.let { current ->
                        val processed = RMSCompute.computeRMS(current, samplesPerBar)
                        currentFrame.addAll(processed)
                    }

                } while (batch != null)
                Log.d("Test" , "Count = ${batch?.size}")
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
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(bars) { amplitude ->
                BarItem(amplitude / maxAmplitude , modifier = Modifier.fillParentMaxWidth(1.0f / bars.size))
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