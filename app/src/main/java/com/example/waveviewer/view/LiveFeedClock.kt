package com.example.waveviewer.view

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing timing and synchronization of waveform refresh cycles.
 * Provides a clock signal, update frequency, and total duration of the refresh cycle.
 */
interface LiveFeedClock {

     /**
      * Returns a Flow that emits clock ticks at a regular interval,
      * which can be used to synchronize updates.
      *
      * @return A Flow of integers representing time ticks.
      */
     fun getClock(): Flow<Long>

     /**
      * Retrieves the refresh frequency in Hz, determining how often the waveform updates.
      *
      * @return The refresh frequency in Hertz.
      */
     fun getDelayTime(): Int

     /**
      * Retrieves the total length of the timing cycle, which may correspond to
      * the duration of the full waveform display.
      *
      * @return The total length of the timing cycle.
      */
     fun getLengthInMs(): Long

     fun pause()

     fun start()

     fun reset()

     fun isRunning()  : Boolean

     enum class ClockState{
          Default,
          Playing,
          Paused,
          Stopped,
          Completed,
          Error
     }

     fun setState(newState : ClockState)

     fun getCurrentState() :ClockState

     fun setProgress(progress : Float)
}
