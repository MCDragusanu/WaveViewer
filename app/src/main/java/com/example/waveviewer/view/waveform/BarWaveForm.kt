package com.example.waveviewer.view.waveform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WaveForm(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    bitDepth: Int,
    frameData: List<Pair<Array<Double>, String>>,
    trackColor: Color = Color.Cyan.copy(alpha = 0.1f),    // Light background color for track
    waveColor: Color = Color.Cyan.copy(alpha = 0.5f), // Default waveform bar color
    labelColor : Color = Color.DarkGray
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(padding)
            .fillMaxWidth()
            .wrapContentHeight()
            .background(trackColor)
    ) {
        val availableWidth = maxWidth
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(frameData) { (bars, label) ->
                val frameWidth: Dp = availableWidth / frameData.size

                Box(
                    modifier = Modifier
                        .width(frameWidth)
                        .height(80.dp)
                ) {

                    // Draw bars
                    BarList(
                        modifier = Modifier.fillMaxSize(),
                        bars = bars,
                        bitDepth = bitDepth,
                        frameWidth = frameWidth,
                        waveColor = waveColor
                    )

                    // Timestamp label
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = label,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            fontSize = 8.sp,
                            color = labelColor
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun BarList(
    modifier: Modifier,
    bars: Array<Double>,
    bitDepth: Int,
    frameWidth: Dp,
    waveColor: Color = Color.Cyan       // Color for waveform bars
) {
    val maxAmplitude = (1 shl (bitDepth - 1)).toDouble() * 0.5 // Max amplitude based on bit depth
    LazyRow(
        modifier = modifier.then(Modifier.fillMaxWidth()),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(bars) { amplitude ->
            BarItem(
                heightRatio = amplitude / maxAmplitude,
                barWidth = (frameWidth / bars.size + 1.dp),
                waveColor = waveColor,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun BarItem(
    heightRatio: Double,
    barWidth: Dp,
    waveColor: Color = Color.Cyan,    // Color for this bar
    modifier: Modifier
) {
    val normalizedHeight = (heightRatio * 100).dp // Scale height proportionally
    Box(
        modifier = modifier.then(
            Modifier
                .height(normalizedHeight.coerceAtLeast(4.dp)) // Ensure bars are visible
                .width(barWidth)
                .background(waveColor)
        )
    )
}
