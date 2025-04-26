package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMSample
import kotlin.math.max

class WavMonoFrame(header : PCMHeader, private val rawBytes : ByteArray) : PCMFrame {

    private val sampleStride = 2
    private val sampleCapacity = max(header.getSampleRate(), rawBytes.size)
    private val sampleCount = rawBytes.size / sampleStride

    companion object{
        val EMPTY_FRAME =  WavMonoFrame(WavHeader(ByteArray(44) ), ByteArray(0))
    }
    override fun getBytes(): ByteArray {
        return rawBytes
    }

    fun getSampleCount(): Int = sampleCount

   override fun get(index: Int): PCMSample {
        if (index < 0 || index >= sampleCount) {
            throw IndexOutOfBoundsException("Sample index $index out of bounds (valid range: 0-${sampleCount - 1})")
        }

        val where = index * sampleStride

        if (where + sampleStride > rawBytes.size) {
            throw PCMError.InvalidIterator("Not enough bytes to read a complete sample at index $index")
        }
       val sample = when (sampleStride) {
            1 -> WavPCMSample.Unsigned8BitSample(rawBytes[where])
            2 -> WavPCMSample.Signed16BitSample(rawBytes[where], rawBytes[where + 1])
            3 -> WavPCMSample.Signed24BitSample(
                rawBytes[where],
                rawBytes[where + 1],
                rawBytes[where + 2]
            )

            else -> throw PCMError.InvalidBitDepth("Unsupported bit depth: ${sampleStride * 8} bits")
        }

        return sample
    }

    override val size: Int
        get() = getSampleCount()

    override fun contains(element: PCMSample): Boolean {
        return false
    }

    override fun containsAll(elements: Collection<PCMSample>): Boolean {
       return false
    }

    override fun isEmpty(): Boolean {
       return sampleCount == 0
    }

    override fun getSampleByteStride(): Int = sampleStride

    override fun getChannelCount(): Int = 1

    override fun getSampleCapacity(): Int = sampleCapacity

    override fun iterator(): Iterator<PCMSample> {
        val sample = get(0)

        return PCMIterator.PCMSampleIterator(this, sample, 0, this.sampleCount)
    }
}