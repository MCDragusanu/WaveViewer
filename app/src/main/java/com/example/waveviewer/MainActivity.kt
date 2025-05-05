package com.example.waveviewer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.waveviewer.audio_stream.pcm.bit_stream.stereo.PCMStreamFactory
import com.example.waveviewer.audio_stream.wav_mono.MonoWavStream
import com.example.waveviewer.audio_stream.wav_stereo.StereoWavStream
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.music_player.MediaPlayerListener
import com.example.waveviewer.ui.theme.WaveViewerTheme
import com.example.waveviewer.view.navigation.MainNavGraph.Main

import com.example.waveviewer.view.waveform.UniversalWaveViewer
import java.io.File
import java.lang.Exception

class MainActivity : ComponentActivity()  {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            WaveViewerTheme {

                Main(){ file->
                    this.getFileFromAssetFd(this , file)
                }
            }
        }
    }


    private fun getFileFromAssetFd(context: Context, assetName: String): File {
        val afd = context.assets.openFd(assetName)
        val file = File.createTempFile("sample_", assetName, context.cacheDir)

        afd.createInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }



}
