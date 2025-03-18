package com.example.waveviewer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.waveviewer.audio_stream.wav.WavInputStream
import com.example.waveviewer.ui.theme.WaveViewerTheme
import com.example.waveviewer.view.WaveForm
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val file = getFileFromAssetFd(this, "gravitational_wave_mono_44100Hz_16bit.wav")
        val wavStream = WavInputStream(file , 1024*2*2)




        setContent {
            WaveViewerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize(),verticalArrangement = Arrangement.Center) {
                        WaveForm.WaveForm(
                            modifier = Modifier.fillMaxWidth().height(200.dp).padding(innerPadding),
                            frames = wavStream.toList()
                        )

                    }
                }
            }
        }
    }
    fun getFileFromAssetFd(context: Context, assetName: String): File {
        val afd = context.assets.openFd(assetName)
        val file = File.createTempFile("temp_", "_$assetName", context.cacheDir)

        afd.createInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file
    }
}

