package com.example.waveviewer.audio_stream.wav_stereo

import android.util.Log
import android.util.Range
import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.bit_stream.stereo.StereoPCMFrame
import com.example.waveviewer.audio_stream.pcm.bit_stream.stereo.StereoPCMStream
import com.example.waveviewer.audio_stream.wav.WavHeader
import com.example.waveviewer.audio_stream.wav_mono.MonoWavFrame
import java.io.File
import java.io.RandomAccessFile

class StereoWavStream(private val file : File) : StereoPCMStream {
    private val pcmHeader: WavHeader
    private var byteOffset = 0
    private var fileStream: RandomAccessFile? = null
    private var currentFrameIndex : Int = 0

    init {

        file.inputStream().use {
            val headerBuff = ByteArray(44)
            it.read(headerBuff)
            pcmHeader = WavHeader(headerBuff)
        }
        byteOffset = 44
    }

    override fun open() {
        if (!file.exists() || !file.isFile || !file.canRead() ) {
            throw PCMError.FileStreamError("Cannot open file: '${file.path}'")
        }
        fileStream = RandomAccessFile(file, "r")
        Log.d("Test" , "Stream open")
    }

    override fun close() {
        fileStream?.close()
        fileStream = null
        Log.d("Test"  , "Stream Closed!")
    }

    override fun getDescriptor(): PCMHeader {
        return pcmHeader
    }

    override fun readNextFrame(sampleCount: Int): StereoPCMFrame? {
        val stream = fileStream ?: return null
        if (pcmHeader.getChannelCount() != 2) return null

        val bitDepthBytes = pcmHeader.getBitDepth() / 8
        val bytesPerStereoSample = 2 * bitDepthBytes
        val maxBytesToRead = sampleCount * bytesPerStereoSample
        val interleavedBuffer = ByteArray(maxBytesToRead)

        return try {
            stream.seek(byteOffset.toLong())
            val bytesRead = stream.read(interleavedBuffer)
            if (bytesRead <= 0) return null

            val actualSamples = bytesRead / bytesPerStereoSample
            if (actualSamples == 0) return null // not enough for even one full stereo sample

            val leftBuffer = ByteArray(actualSamples * bitDepthBytes)
            val rightBuffer = ByteArray(actualSamples * bitDepthBytes)

            for (i in 0 until actualSamples) {
                val baseIndex = i * bytesPerStereoSample
                interleavedBuffer.copyInto(leftBuffer, i * bitDepthBytes, baseIndex, baseIndex + bitDepthBytes)
                interleavedBuffer.copyInto(rightBuffer, i * bitDepthBytes, baseIndex + bitDepthBytes, baseIndex + bytesPerStereoSample)
            }

            byteOffset += actualSamples * bytesPerStereoSample

            val leftFrame = MonoWavFrame(pcmHeader, leftBuffer)
            val rightFrame = MonoWavFrame(pcmHeader, rightBuffer)
            StereoPCMFrame(leftFrame, rightFrame)

        } catch (e: Exception) {
            e.printStackTrace()
            throw PCMError.UnknownError("Error reading frame from '${file.name}': ${e.message}")
        }
    }







    override fun isOpen(): Boolean {
      return fileStream!=null
    }

    override fun setProgress(progress: Float) {
        if (progress < 0.0f || progress > 1.0f) {
            throw IllegalArgumentException("Progress must be between 0.0 and 1.0")
        }

        val bitDepthBytes = pcmHeader.getBitDepth() / 8
        val bytesPerStereoSample = 2 * bitDepthBytes
        val totalDataLength = pcmHeader.getSampleCount()

        // Compute aligned byte offset within the data section (excluding the 44-byte header)
        var byteOffsetInData = (progress * totalDataLength).toInt()

        // Align to the nearest lower multiple of a full stereo sample
        byteOffsetInData -= byteOffsetInData % bytesPerStereoSample

        // Update internal offset, adding 44 bytes for WAV header
        byteOffset = 44 + byteOffsetInData

        // Optional: update current frame index
        currentFrameIndex = byteOffsetInData / bytesPerStereoSample
    }
}