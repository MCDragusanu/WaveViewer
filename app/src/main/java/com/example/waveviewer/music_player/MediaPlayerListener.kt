package com.example.waveviewer.music_player

import java.lang.Exception

/**
 * Listener interface for handling media player state changes and events.
 */
interface MediaPlayerListener {

    /** Called when a data stream has been successfully assigned. */
    fun onDataStreamAssigned()

    /** Called when the media player is paused. */
    fun onPaused()

    /** Called when the media player starts playing. */
    fun onStarted()

    /** Called when an error occurs. */
    fun onError(exception: Exception)

    /** Called when playback has reached the end. */
    fun onPlaybackEnded()

    fun onSeekCompleted()

    fun onPrepared()

    fun onStopped()
}