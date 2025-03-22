import android.util.Log
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import com.example.waveviewer.view.LiveFeedClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.math.roundToLong

class AudioClock(file: PCMInputStream) : LiveFeedClock {
    private val totalLengthInMs: Long
    private var currentTick: Long = 0
    private var currentState = MutableStateFlow(LiveFeedClock.ClockState.Default)

    init {
        val sampleRate = file.getDescriptor().getSampleRate()
        val sampleCount = (file.size - file.getDescriptor().getHeaderSize()) / (file.getDescriptor().getBitDepth() / 8)
        val seconds = (sampleCount / sampleRate).toFloat()
        totalLengthInMs = (seconds * 1000).roundToLong()
        Log.d("Test", "Computation: sampleRate=$sampleRate, sampleCount=$sampleCount, seconds=$seconds")
    }

    override fun getClock(): Flow<Long> = flow {
        while (true) {
            when (currentState.value) {
                LiveFeedClock.ClockState.Playing -> {
                    emit(currentTick)
                    currentTick += getDelayTime()
                    delay(getDelayTime().toLong())
                }
                LiveFeedClock.ClockState.Paused -> {
                    delay(100)
                }
                LiveFeedClock.ClockState.Stopped -> {
                    currentTick = 0
                    return@flow
                }
                else -> {
                    delay(100)
                }
            }
        }
    }.distinctUntilChanged()

    override fun getDelayTime(): Int = 1000 / 60 // 120Hz

    override fun getLengthInMs(): Long = totalLengthInMs

    override fun pause() {
        currentState.value = LiveFeedClock.ClockState.Paused
    }

    override fun start() {
        if (currentState.value != LiveFeedClock.ClockState.Playing) {
            currentState.value = LiveFeedClock.ClockState.Playing
        }
    }

    override fun reset() {
        currentTick = 0
    }

    override fun isRunning(): Boolean {
        return currentState.value == LiveFeedClock.ClockState.Playing
    }

    override fun setState(newState: LiveFeedClock.ClockState) {
        currentState.value = newState
    }

    override fun getCurrentState(): LiveFeedClock.ClockState {
        return currentState.value
    }

    override fun setProgress(progress: Float) {
        currentTick = (getLengthInMs() * progress).toLong()
    }
}
