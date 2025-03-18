import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import com.example.waveviewer.audio_stream.pcm.PCMSample
import com.example.waveviewer.audio_stream.wav.WavMonoFrame

sealed class PCMIterator<T>(
    protected var value: T,
    protected var currentIndex: Int,
    protected val size: Int
) :
    Iterator<T> {

    class PCMFrameIterator(
        private val stream: PCMInputStream,
        frame: PCMFrame,
        currentIndex: Int,
        endIndex: Int
    ) : PCMIterator<PCMFrame>(frame, currentIndex, endIndex) {

        override fun hasNext(): Boolean {
            return currentIndex < size
        }

        override fun next(): PCMFrame {
            if (!hasNext()) {
                stream.close()
                throw NoSuchElementException("No more frames available")
            }

            val frame = stream.readNextFrame(stream.getDescriptor().getSampleRate()) ?: WavMonoFrame(stream.getDescriptor() , rawBytes = ByteArray(0))
               // ?: throw NoSuchElementException("Unexpected end of stream")

            value = frame
            currentIndex++

            if (!hasNext()) {
                stream.close()
            }

            return frame
        }
    }

    class PCMSampleIterator(
        private val frame: PCMFrame,
        sample: PCMSample,
        currentIndex: Int,
        endIndex: Int
    ) : PCMIterator<PCMSample>(sample, currentIndex, endIndex) {
        override fun hasNext(): Boolean {
            return currentIndex * frame.getSampleByteStride() < frame.getBytes().size
        }

        override fun next(): PCMSample {
            val sample = frame.get(currentIndex)
            currentIndex++
            return sample
        }
    }
}
