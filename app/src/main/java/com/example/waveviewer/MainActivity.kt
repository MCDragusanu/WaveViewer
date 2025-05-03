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
import com.example.waveviewer.audio_stream.wav_mono.MonoWavStream
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.music_player.MediaPlayerListener
import com.example.waveviewer.ui.theme.WaveViewerTheme
import com.example.waveviewer.view.waveform.MonoWaveViewer
import java.io.File
import java.lang.Exception

class MainActivity : ComponentActivity() , MediaPlayerListener {

    lateinit var mediaPlayerAdapter: MediaPlayerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaPlayerAdapter = MediaPlayerAdapter(this, this)
        val file = getFileFromAssetFd(this, "music_mono_44100Hz_16bit.wav")
        val wavStream = MonoWavStream(file)
        wavStream.open()
        mediaPlayerAdapter.setAudioFile(file.path)
        setContent {
            WaveViewerTheme {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    Column(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MonoWaveViewer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            songDurationInMs = mediaPlayerAdapter.getLengthInMs().toInt(),
                            windowSize = 8,
                            stream = wavStream
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayerAdapter.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerAdapter.stop()
    }

    override fun onRestart() {
        super.onRestart()
        mediaPlayerAdapter = MediaPlayerAdapter(this, this)
        val file = getFileFromAssetFd(this, "music_mono_44100Hz_16bit.wav")
        mediaPlayerAdapter.setAudioFile(file.path)
    }

    override fun onResume() {
        super.onResume()
        if(::mediaPlayerAdapter.isInitialized){

        }
    }
    private fun getFileFromAssetFd(context: Context, assetName: String): File {
        val afd = context.assets.openFd(assetName)
        val file = File.createTempFile("temp_", "_$assetName", context.cacheDir)

        afd.createInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    override fun onDataStreamAssigned() {

    }

    override fun onPaused() {

    }

    override fun onStarted() {

    }

    override fun onError(exception: Exception) {
        exception.printStackTrace()
    }

    override fun onPlaybackEnded() {

    }

    override fun onSeekCompleted() {

    }

    override fun onPrepared() {

    }

    override fun onStopped() {

    }

}
