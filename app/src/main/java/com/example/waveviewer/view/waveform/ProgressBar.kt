package com.example.waveviewer.view.waveform

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressBar(modifier : Modifier, currentProgress :()->Float,onProgressChanged : (Float)->Unit ){

    Box(modifier = modifier , contentAlignment = Alignment.Center){
        Slider(value = currentProgress(), onValueChange = onProgressChanged, modifier = modifier)

    }
}