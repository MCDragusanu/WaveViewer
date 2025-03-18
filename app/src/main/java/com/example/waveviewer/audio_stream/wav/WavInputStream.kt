package com.example.waveviewer.audio_stream.wav

import PCMIterator
import android.util.Log
import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.min

class WavInputStream(private val file: File, private val samplesPerFrame : Int = 44100) : PCMInputStream() {

    private val pcmHeader: WavHeader
    private var byteOffset = 0
    private var fileStream: RandomAccessFile? = null
    private var frameCount : Int
    private var currentFrameIndex : Int = 0
    init {
        val headerBuff = ByteArray(44)
        file.inputStream().use {
            it.read(headerBuff)
            frameCount = it.available() / samplesPerFrame
        }
        pcmHeader = WavHeader(headerBuff)

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

    override fun readNextFrame(sampleCount: Int): PCMFrame? {
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
            WavMonoFrame(header = pcmHeader, rawBytes = buffer.array())

        } catch (e: Exception) {
            e.printStackTrace()
            throw PCMError.UnknownError("Error reading frame from '${file.name}': ${e.message}")
        }
    }



    override fun iterator(): Iterator<PCMFrame> {
        // Reset file stream to the beginning of the data section
        byteOffset = pcmHeader.getHeaderSize()
        currentFrameIndex = 0
        open()
        val firstFrame = readNextFrame(samplesPerFrame) ?: return emptyList<PCMFrame>().iterator()

        return PCMIterator.PCMFrameIterator(this, firstFrame, currentFrameIndex, frameCount)
    }


}
