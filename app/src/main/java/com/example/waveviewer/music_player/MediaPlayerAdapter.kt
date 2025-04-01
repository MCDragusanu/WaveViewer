package com.example.waveviewer.music_player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.waveviewer.view.LiveFeedClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class MediaPlayerAdapter(context: Context, private val listener: MediaPlayerListener) : LiveFeedClock {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentState: LiveFeedClock.ClockState = LiveFeedClock.ClockState.Default

    init {
        setupMediaPlayerListeners()
    }

    /**
     * Initializes MediaPlayer event listeners for handling playback events.
     */
    private fun setupMediaPlayerListeners() {
        mediaPlayer.apply {
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer Error: Code=$what, Extra=$extra")
                listener.onError(Exception("Error Code: $what; Extra: $extra"))
                currentState = LiveFeedClock.ClockState.Error
                true
            }

            setOnCompletionListener {
                Log.d(TAG, "Playback Completed")
                listener.onPlaybackEnded()
                currentState = LiveFeedClock.ClockState.Completed
            }

            setOnPreparedListener {
                Log.d(TAG, "MediaPlayer Prepared")
                listener.onPrepared()
                currentState = LiveFeedClock.ClockState.Default
            }

            setOnSeekCompleteListener {
                Log.d(TAG, "Seek Completed")
                listener.onSeekCompleted()
                //currentState = LiveFeedClock.ClockState.Default
            }
        }
    }

    override fun getClock(): Flow<Long> = flow {
        while (true) {
            when (currentState) {
                LiveFeedClock.ClockState.Playing -> {
                    if (mediaPlayer.isPlaying) {
                        Log.d("Test"  ,"Media Player Pos : ${mediaPlayer.currentPosition} ms")
                        emit(mediaPlayer.currentPosition.toLong())
                    } else {
                        Log.w(TAG, "State Playing but MediaPlayer is not playing")
                    }
                    delay(getDelayTime().toLong())
                }
                LiveFeedClock.ClockState.Paused, LiveFeedClock.ClockState.Stopped -> delay(100)
                else -> delay(100)
            }
        }
    }.distinctUntilChanged()

    override fun getDelayTime(): Int = 1000 / 60

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
                Log.d(TAG, "Playback Paused")
            } else {
                Log.w(TAG, "Pause called but MediaPlayer is not playing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media: ${e.message}")
        }
    }
     fun stop() {
        try {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                currentState = LiveFeedClock.ClockState.Stopped
                Log.d(TAG, "Playback Stopped")
            } else {
                Log.w(TAG, "Stop called but MediaPlayer is not playing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media: ${e.message}")
        }
    }

    override fun start() {
        try {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                currentState = LiveFeedClock.ClockState.Playing
                Log.d(TAG, "Playback Started")
            } else {
                Log.w(TAG, "Start called but MediaPlayer is already playing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting media: ${e.message}")
        }
    }

    override fun reset() {
        try {
            mediaPlayer.seekTo(0)
            currentState = LiveFeedClock.ClockState.Default
            Log.d(TAG, "Playback Reset to Start")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting media: ${e.message}")
        }
    }

    override fun isRunning(): Boolean = currentState == LiveFeedClock.ClockState.Playing

    override fun setState(newState: LiveFeedClock.ClockState) {
        Log.d(TAG, "State changed: $currentState → $newState")
        currentState = newState
    }

    override fun getCurrentState(): LiveFeedClock.ClockState = currentState

    override fun setProgress(progress: Float) {
        try {
            val msOffset = (progress * getLengthInMs()).toLong()
            mediaPlayer.seekTo(msOffset ,  MediaPlayer.SEEK_CLOSEST_SYNC)
            Log.d(TAG, "Seeked to $msOffset ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }

    fun setAudioFile(path: String) {
        try {
            mediaPlayer.reset() // Ensure media player is in a fresh state
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepareAsync()
            Log.d(TAG, "Audio File Set: $path")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio file: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "Test"
    }
}
