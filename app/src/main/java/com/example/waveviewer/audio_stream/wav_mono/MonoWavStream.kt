package com.example.waveviewer.audio_stream.wav_mono

import android.util.Log
import android.util.Range
import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMFrame
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMStream
import com.example.waveviewer.audio_stream.wav.WavHeader
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.min

class MonoWavStream(private val file: File,
                    private val samplesPerFrame : Int = 44100) : MonoPCMStream() {


    private val pcmHeader: WavHeader
    private var byteOffset = 0
    private var fileStream: RandomAccessFile? = null
    private var frameCount : Int
    private var currentFrameIndex : Int = 0

    init {

        file.inputStream().use {
            val headerBuff = ByteArray(44)
            it.read(headerBuff)
            pcmHeader = WavHeader(headerBuff)
        }

        frameCount = pcmHeader.getSampleCount() / samplesPerFrame
    }

    override fun open() {
        if (!file.exists() || !file.isFile || !file.canRead() ) {
            throw PCMError.FileStreamError("Cannot open file: '${file.path}'")
        }
        fileStream = RandomAccessFile(file, "r")
        resetReading()
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


    override fun isOpen() : Boolean{
        return this.fileStream!=null
    }

    override fun setProgress(progress: Float) {
        try {

            // Ensure progress is within valid range
            val clampedProgress = progress.coerceIn(0f, 1f)

            // Calculate the exact byte offset based on progress
            val totalSampleByteSize = file.length() - pcmHeader.getHeaderSize()
            val bytePosition = (clampedProgress * totalSampleByteSize).toInt()

            // Align to the nearest sample (to avoid reading partial samples)
            val bytesPerSample = pcmHeader.getBitDepth() / 8

            val totalSamples = (totalSampleByteSize) / bytesPerSample
            val durationMs= ((totalSamples / pcmHeader.getSampleRate()) * 1000) * progress

            val alignedByteOffset = (bytePosition / bytesPerSample) * bytesPerSample

            // Set the new byte offset, making sure we don’t seek before header
            byteOffset = pcmHeader.getHeaderSize() + alignedByteOffset


            // Seek the file stream to the new position
            fileStream?.seek(byteOffset.toLong())

        } catch (e: Exception) {
            Log.e("Test", "Error seeking: ${e.message}")
        }
    }

    override fun readNextFrame(sampleCount: Int): MonoPCMFrame? {
        val stream = fileStream ?: return null

        if (pcmHeader.getChannelCount() > 1) {
            TODO("Implement multi-channel support")
        }

        return try {
            stream.seek(byteOffset.toLong())

            val frameByteSize = min(
                sampleCount * pcmHeader.getBitDepth() / 8,
                (stream.length() - pcmHeader.getHeaderSize() - byteOffset).toInt()
            )

            if (frameByteSize <= 0) return null

            val buffer = ByteBuffer.allocate(frameByteSize)
            val bytesReadNow = stream.read(buffer.array())

            if (bytesReadNow == -1) return null

            byteOffset += bytesReadNow
            MonoWavFrame(header = pcmHeader, rawBytes = buffer.array())

        } catch (e: Exception) {
            e.printStackTrace()
            throw PCMError.UnknownError("Error reading frame from '${file.name}': ${e.message}")
        }
    }

    private fun resetReading(){
        byteOffset = pcmHeader.getHeaderSize()
        currentFrameIndex = 0

    }



}
