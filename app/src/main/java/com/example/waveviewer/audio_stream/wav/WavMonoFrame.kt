package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMSample

class WavMonoFrame(header : PCMHeader, private val rawBytes : ByteArray) : PCMFrame {
    private val sampleStride = header.getBitDepth() / 8
    private val sampleCount = rawBytes.size / sampleStride
    override fun getSamples(): ByteArray {
        return rawBytes
    }

    override fun getSampleCount(): Int = sampleCount

    override fun get(index: Int): PCMSample {
        val where = index * sampleStride
        if (where > rawBytes.lastIndex - sampleStride + 1)
            throw PCMError.InvalidIterator("Iterator is out of bounds : trying to access byte of index ${where} in buffer of length ${rawBytes.size}")

        return when (sampleStride) {
            1 -> WavPCMSample.Unsigned8BitSample(rawBytes[where]) // 8-bit PCM is unsigned in WAV
            2 -> WavPCMSample.Signed16BitSample(rawBytes[where] , rawBytes[where + 1])// 16-bit PCM (Little-Endian)
            3 -> WavPCMSample.Signed24BitSample(rawBytes[where] , rawBytes[where + 1] , rawBytes[where + 2])
            else -> throw PCMError.InvalidBitDepth("Unsupported bit depth: ${sampleStride * 8} bits")
        }
    }

    override fun getSampleStride(): Int = sampleStride
    override fun getChannelCount(): Int {
        return 1
    }

    override fun getSampleCapacity(): Int {
        return 44100
    }
}