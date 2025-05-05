package com.example.waveviewer.view.screens.waveform

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamFactory
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamWrapper
import com.example.waveviewer.content_provider.ContentResolverAdapter
import com.example.waveviewer.music_player.LiveFeedClock
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.music_player.MediaPlayerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.io.File
import java.lang.Exception
import java.util.UUID

/**
 * ViewModel for the WaveForm screen that manages audio playback and waveform visualization
 */
class WaveFormViewModel : ViewModel(), MediaPlayerListener {

    private lateinit var mediaPlayerAdapter: MediaPlayerAdapter
    private lateinit var contentResolverAdapter: ContentResolverAdapter

    // Audio stream for waveform visualization
    private val _audioStream = MutableStateFlow<PCMStreamWrapper?>(null)
    val audioStream: StateFlow<PCMStreamWrapper?> = _audioStream.asStateFlow()

    // Current playback progress percentage (0.0-1.0)
    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent.asStateFlow()

    // Total duration of the audio file in milliseconds
    private val _mediaLength = MutableStateFlow(0L)
    val mediaLength: StateFlow<Long> = _mediaLength.asStateFlow()

    // Current state of the media player
    private val _mediaPlayerState = MutableStateFlow(LiveFeedClock.ClockState.Default)
    val mediaPlayerState: StateFlow<LiveFeedClock.ClockState> = _mediaPlayerState.asStateFlow()

    /**
     * Sets the ContentResolverAdapter for file URI resolution
     */
    fun setContentResolver(contentResolverAdapter: ContentResolverAdapter) {
        this.contentResolverAdapter = contentResolverAdapter
    }

    /**
     * Loads audio from a URI and prepares it for playback
     */
    fun setUri(fileUri: Uri, context: Context, onFailedToLoadUri: () -> Unit ) {
        try {
            // First try direct file access
            val file = fileUri.toFile()
            loadFileAndPreparePlayer(file)
        } catch (e: IllegalArgumentException) {
            // Fall back to content resolver
            val name = fileUri.path?.split("/")?.last()?:"Sample"
            val destFile  = File(context.dataDir, "/example${UUID.randomUUID()}.wav")
            val filePath = contentResolverAdapter.readIntoTempFile(uri = fileUri , destFile)
            if (filePath == null) {
                Log.e("WaveFormViewModel", "Failed to resolve file path from URI", e)
                onFailedToLoadUri()
                return
            }
            filePath.createNewFile()

            loadFileAndPreparePlayer(filePath)
        } catch (e: Exception) {
            Log.e("WaveFormViewModel", "Error loading file", e)
            onFailedToLoadUri()
            _mediaPlayerState.update { LiveFeedClock.ClockState.Error }
        }
    }

    /**
     * Helper method to load file and prepare the player
     */
    private fun loadFileAndPreparePlayer(file: File) {
        if (!file.exists()) {
            Log.e("WaveFormViewModel", "File does not exist: ${file.path}")
            _mediaPlayerState.update { LiveFeedClock.ClockState.Error }
            return
        }

        Log.d("WaveFormViewModel", "Loading file: ${file.name}, size: ${file.length()}")

        // Create PCM stream for waveform visualization
        _audioStream.update {
            PCMStreamFactory.getInstance().provideBitStream(file)
        }

        // Set the audio file in the media player
        mediaPlayerAdapter.setAudioFile(file.path)
    }

    /**
     * Sets the MediaPlayerAdapter and configures listeners
     */
    fun setMediaPlayer(mediaPlayerAdapter: MediaPlayerAdapter) {
        this.mediaPlayerAdapter = mediaPlayerAdapter
        mediaPlayerAdapter.setListener(this)

        // Observe clock ticks from player and update progress
        mediaPlayerAdapter.getClock()
            .onEach { timeMs ->
                val duration = mediaPlayerAdapter.getLengthInMs()
                if (duration > 0) {
                    val percent = timeMs / duration.toFloat()
                    _progressPercent.update { percent }
                }
            }
            .launchIn(viewModelScope)
    }

    // Playback control methods

    fun pause() {
        mediaPlayerAdapter.pause()
    }

    fun stop() {
        mediaPlayerAdapter.stop()
    }

    fun start() {
        mediaPlayerAdapter.start()
    }

    /**
     * Sets playback position to a percentage (0.0-1.0) of total duration
     */
    fun setProgress(percent: Float) {
        val clampedPercent = percent.coerceIn(0f, 1f)
        Log.d("WaveFormViewModel", "Setting progress: $clampedPercent")

        mediaPlayerAdapter.setProgress(clampedPercent)
        _audioStream.value?.setProgress(clampedPercent)
    }

    // Media player event callbacks

    override fun onDataStreamAssigned() {
        Log.d("WaveFormViewModel", "Audio file assigned")
        _mediaLength.update {
            mediaPlayerAdapter.getLengthInMs()
        }
        _mediaPlayerState.update { LiveFeedClock.ClockState.Ready }
    }

    override fun onPaused() {
        _mediaPlayerState.update { LiveFeedClock.ClockState.Paused }
    }

    override fun onStarted() {
        _mediaPlayerState.update { LiveFeedClock.ClockState.Playing }
    }

    override fun onError(exception: Exception) {
        Log.e("WaveFormViewModel", "Player error", exception)
        _mediaPlayerState.update { LiveFeedClock.ClockState.Error }
    }

    override fun onPlaybackEnded() {
        _mediaPlayerState.update { LiveFeedClock.ClockState.Completed }
    }

    override fun onSeekCompleted() {
        _mediaPlayerState.update { LiveFeedClock.ClockState.Ready }
    }

    override fun onPrepared() {
        _mediaLength.update {
            mediaPlayerAdapter.getLengthInMs()
        }
        _mediaPlayerState.update { LiveFeedClock.ClockState.Ready }
    }

    override fun onStopped() {
        _mediaPlayerState.update { LiveFeedClock.ClockState.Stopped }
    }
}