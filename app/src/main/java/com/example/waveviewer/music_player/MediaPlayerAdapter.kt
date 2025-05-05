package com.example.waveviewer.music_player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * MediaPlayerAdapter provides a wrapper around Android's MediaPlayer with additional
 * functionality for clock-based tracking and event handling.
 */
class MediaPlayerAdapter() : LiveFeedClock {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentState: LiveFeedClock.ClockState = LiveFeedClock.ClockState.Default
    private var listener: MediaPlayerListener? = null

    companion object {
        private const val TAG = "MediaPlayerAdapter"
        private const val DEFAULT_UPDATE_RATE = 144 // 60 fps
    }

    init {
        setupMediaPlayerListeners()
    }

    fun setListener(listener: MediaPlayerListener) {
        this.listener = listener
    }

    private fun setupMediaPlayerListeners() {
        mediaPlayer.apply {
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer Error: Code=$what, Extra=$extra")
                listener?.onError(Exception("Error Code: $what; Extra: $extra"))
                currentState = LiveFeedClock.ClockState.Error
                true
            }

            setOnCompletionListener {
                Log.d(TAG, "Playback Completed")
                listener?.onPlaybackEnded()
                currentState = LiveFeedClock.ClockState.Completed
            }

            setOnPreparedListener {
                Log.d(TAG, "MediaPlayer Prepared")
                listener?.onPrepared()
                currentState = LiveFeedClock.ClockState.Default
            }

            setOnSeekCompleteListener {
                Log.d(TAG, "Seek Completed")
                listener?.onSeekCompleted()
                // State is intentionally not changed here
            }
        }
    }

    override fun getClock(): Flow<Long> = flow {
        while (true) {
            when (currentState) {
                LiveFeedClock.ClockState.Playing -> {
                    if (mediaPlayer.isPlaying) {
                        emit(mediaPlayer.currentPosition.toLong())
                    } else {
                        Log.w(TAG, "State is Playing but MediaPlayer is not actually playing")
                    }
                    delay(getDelayTime().toLong())
                }
                else -> delay(100) // Reduced polling rate when not playing
            }
        }
    }.distinctUntilChanged()

    override fun getDelayTime(): Int = 1000 / DEFAULT_UPDATE_RATE

    override fun getLengthInMs(): Long = try {
        mediaPlayer.duration.toLong()
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Failed to get duration: ${e.message}")
        0L
    }

    override fun pause() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                currentState = LiveFeedClock.ClockState.Paused
                Log.d(TAG, "Playback paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media: ${e.message}")
        }
    }

    fun stop() {
        try {
            mediaPlayer.stop()
            currentState = LiveFeedClock.ClockState.Stopped
            Log.d(TAG, "Playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media: ${e.message}")
        }
    }

    override fun start() {
        try {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                currentState = LiveFeedClock.ClockState.Playing
                Log.d(TAG, "Playback started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting media: ${e.message}")
        }
    }

    override fun reset() {
        try {
            mediaPlayer.seekTo(0)
            currentState = LiveFeedClock.ClockState.Default
            Log.d(TAG, "Playback reset to start")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting media: ${e.message}")
        }
    }

    override fun isRunning(): Boolean = currentState == LiveFeedClock.ClockState.Playing

    override fun setState(newState: LiveFeedClock.ClockState) {
        Log.d(TAG, "State changing: $currentState → $newState")
        currentState = newState
    }


    override fun getCurrentState(): LiveFeedClock.ClockState = currentState

    override fun setProgress(progress: Float) {
        try {
            if (progress !in 0f..1f) {
                Log.w(TAG, "Invalid progress value: $progress (should be 0.0-1.0)")
                return
            }

            val msOffset = (progress * getLengthInMs()).toLong()
            mediaPlayer.seekTo(msOffset, MediaPlayer.SEEK_CLOSEST_SYNC)
            Log.d(TAG, "Seeking to $msOffset ms (${progress * 100}%)")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }


    fun setAudioFile(path: String) {
        try {
            val file = File(path)
            require(file.exists())
            mediaPlayer.reset() // Ensure media player is in a fresh state
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            Log.d(TAG, "Audio file set: $path")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio file: ${e.message}")
            listener?.onError(e)
        }
    }


    fun release() {
        try {
            mediaPlayer.release()
            Log.d(TAG, "MediaPlayer released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
        }
    }
}