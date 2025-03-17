package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeader(byteArray: ByteArray) : PCMHeader {

    private val rawBytes: ByteArray = byteArray

    private val chunkId: String = String(rawBytes, 0, 4)  // "RIFF"
    private val format: String = String(rawBytes, 8, 4)   // "WAVE"
    private val subchunk1Size: Int = readIntLE(16)        // 16 for PCM
    private val audioFormat: Int = readShortLE(20)        // 1 = PCM
    private val numChannels: Int = readShortLE(22)        // 1 = Mono, 2 = Stereo
    private val sampleRate: Int = readIntLE(24)           // 44100, 48000, etc.
    private val byteRate: Int = readIntLE(28)             // SampleRate * NumChannels * BitsPerSample/8
    private val blockAlign: Int = readShortLE(32)         // NumChannels * BitsPerSample/8
    private val bitsPerSample: Int = readShortLE(34)      // 8, 16, 24, 32
    private val dataChunkId: String = String(rawBytes, 36, 4)  // "data"
    private val dataSize: Int = readIntLE(40)             // Size of audio data

    private fun readShortLE(offset: Int): Int {
        return ByteBuffer.wrap(rawBytes, offset, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short.toInt() and 0xFFFF
    }

    private fun readIntLE(offset: Int): Int {
        return ByteBuffer.wrap(rawBytes, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    override fun getBitDepth(): Int = bitsPerSample

    override fun getBitRate(): Int = byteRate * 8

    override fun getSampleRate(): Int = sampleRate

    override fun getChannelCount(): Int = numChannels

    override fun getHeaderSize(): Int = 44

    override fun getSampleCount(): Int = dataSize

    fun isValid(): Boolean {
        return chunkId == "RIFF" && format == "WAVE" && dataChunkId == "data" && audioFormat == 1
    }

    fun getBytes(): ByteArray {
        return rawBytes
    }
}
