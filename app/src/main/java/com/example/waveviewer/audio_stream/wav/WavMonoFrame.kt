package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMSample
import kotlin.math.max

class WavMonoFrame(header : PCMHeader, private val rawBytes : ByteArray) : PCMFrame {

    private val sampleStride = header.getBitDepth() / 8
    private val sampleCapacity = max(header.getSampleRate(), rawBytes.size)
    private val sampleCount = rawBytes.size / sampleStride

    override fun getBytes(): ByteArray {
        return rawBytes
    }

    fun getSampleCount(): Int = sampleCount

   override fun get(index: Int): PCMSample {
        // Validate index boundaries
        if (index < 0 || index >= sampleCount) {
            throw IndexOutOfBoundsException("Sample index $index out of bounds (valid range: 0-${sampleCount - 1})")
        }

        val where = index * sampleStride

        // Validate that there's enough bytes remaining for a complete sample
        if (where + sampleStride > rawBytes.size) {
            throw PCMError.InvalidIterator("Not enough bytes to read a complete sample at index $index")
        }

        // Create the sample
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
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<PCMSample>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSampleByteStride(): Int = sampleStride

    override fun getChannelCount(): Int = 1

    override fun getSampleCapacity(): Int = sampleCapacity

    override fun iterator(): Iterator<PCMSample> {
        val sample = object : PCMSample {
            override fun getValue(): Int {
                return -1
            }
        }
        return PCMIterator.PCMSampleIterator(this, sample, 0, this.sampleCount)
    }
}