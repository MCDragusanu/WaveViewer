package com.example.waveviewer.view.screens.waveform

import androidx.lifecycle.ViewModel
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamWrapper
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.music_player.MediaPlayerListener
import java.lang.Exception

class WaveFormViewModel:ViewModel() , MediaPlayerListener {
    private lateinit var audioStream : PCMStreamWrapper
    private lateinit var mediaPlayerAdapter: MediaPlayerAdapter

    fun setUri(encodedPath : String){

    }

    fun setMediaPlayer()
    override fun onDataStreamAssigned() {
        TODO("Not yet implemented")
    }

    override fun onPaused() {
        TODO("Not yet implemented")
    }

    override fun onStarted() {
        TODO("Not yet implemented")
    }

    override fun onError(exception: Exception) {
        TODO("Not yet implemented")
    }

    override fun onPlaybackEnded() {
        TODO("Not yet implemented")
    }

    override fun onSeekCompleted() {
        TODO("Not yet implemented")
    }

    override fun onPrepared() {
        TODO("Not yet implemented")
    }

    override fun onStopped() {
        TODO("Not yet implemented")
    }
}