package com.example.waveviewer

import AudioClock
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import com.example.waveviewer.audio_stream.wav.WavInputStream
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.music_player.MediaPlayerListener
import com.example.waveviewer.ui.theme.WaveViewerTheme
import com.example.waveviewer.view.BarWaveForm
import com.example.waveviewer.view.DefaultAudioPannel
import com.example.waveviewer.view.LiveFeedClock
import com.example.waveviewer.view.WaveForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() , MediaPlayerListener {

    lateinit var mediaPlayerAdapter: MediaPlayerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaPlayerAdapter = MediaPlayerAdapter(this , this)
        val file = getFileFromAssetFd(this, "whistle_mono_44100Hz_16bit.wav")
        val wavStream = WavInputStream(file)
        wavStream.open()
        mediaPlayerAdapter.setAudioFile(file.path)
        setContent {
            WaveViewerTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BarWaveForm.BarWaveForm(
                        clock = mediaPlayerAdapter,
                        sampleRate = wavStream.getDescriptor().getSampleRate(),
                        audioPlayer = DefaultAudioPannel,
                        bitDepth = wavStream.getDescriptor().getBitDepth(),
                        jumpToPosition = { progress ->
                           wavStream.setProgress(progress)
                            //mediaPlayerAdapter.setProgress(progress)
                        }
                    ) { sampleCount->
                        wavStream.readNextFrame(sampleCount)
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
