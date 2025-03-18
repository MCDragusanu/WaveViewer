package com.example.waveviewer.view

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMSample
import kotlin.math.pow

object WaveForm {

    @Composable
    fun WaveForm(modifier: Modifier, frames: Collection<PCMFrame>) {
        LazyRow(modifier = modifier) {
            items(frames.count()) { index ->
                WaveFormElement(
                    modifier = Modifier.width(40.dp).height(200.dp),
                    frames.elementAt(index),
                    frames.elementAt(index).getSampleByteStride() * 8
                )
            }
        }

    }

    @Composable
    fun WaveFormElement(modifier: Modifier, frame: Collection<PCMSample> , bitDepth : Int)  {



        val samples = frame.toList()
        if(samples.isEmpty()) return
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
}