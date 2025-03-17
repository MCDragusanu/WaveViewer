package com.example.waveviewer.audio_stream.wav

import android.util.Log
import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import com.example.waveviewer.audio_stream.pcm.PCMIterator
import java.io.File
import java.io.RandomAccessFile

class WavInputStream(private val file: File) : PCMInputStream() {
    private var bytesRead = 0
    private lateinit var pcmHeader: WavHeader
    private var pcmIterator: PCMIterator = PCMIterator.Invalid
    private var fileStream: RandomAccessFile? = null

    init {
       file.inputStream().use { stream->
           val headerBuff = ByteArray(44)
           stream.read(headerBuff)
           pcmHeader = WavHeader(headerBuff)
           if(pcmHeader.isValid()){
               pcmIterator = PCMIterator.Begin
           }else {
               pcmIterator = PCMIterator.Invalid
               throw  PCMError.InvalidHeader("WAV Header is invalid!")
           }
       }
    }

    override fun open() {
        if (!file.exists() || !file.isFile || !file.canRead()) {
            pcmIterator = PCMIterator.Invalid
            throw PCMError.FileStreamError("Cannot open file: '${file.path}'")
        }
        fileStream = RandomAccessFile(file, "r")

    }

    override fun close() {
        fileStream?.close()
        fileStream = null
        Log.d("TEST", "Processed $bytesRead bytes from file ${file.name}")
    }

    override fun getDescriptor(): PCMHeader {
        return pcmHeader
    }

    override fun readNextFrame(): PCMFrame? {
        val stream = fileStream ?: return null

        if (pcmIterator == PCMIterator.Invalid){
            throw PCMError.InvalidIterator("Iterator is Invalid : WAV header is not properly formatted. Cannot process file : ${file.name}")
        }

        if (pcmIterator == PCMIterator.End) return null

        if (pcmHeader.getChannelCount() > 1) {
            TODO("Implement multi-channel support")
        }

        try {
            val offset = pcmHeader.getHeaderSize() + translateIteratorPosition(pcmIterator)
            stream.seek(offset)

            val frameByteSize = calculateFrameSize()
            if (frameByteSize < 0) {
                return null
            }
            val buffer = ByteArray(frameByteSize.toInt())
            val bytesReadNow = stream.read(buffer)
            if (bytesReadNow == -1) {
                pcmIterator = PCMIterator.End
                return null
            }

            bytesRead += bytesReadNow
            return WavMonoFrame(header = pcmHeader, rawBytes = buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw PCMError.UnknownError(e.message ?: "Unknown error ocurred reading frame from ${file.name}")

        }

    }

    override fun readNextFrame(pcmIterator: PCMIterator): PCMFrame? {
        if (this.pcmIterator == PCMIterator.Invalid) return null
        moveCursor(pcmIterator)
        return readNextFrame()
    }

    override fun moveCursor(pcmIterator: PCMIterator) {
        this.pcmIterator = pcmIterator
    }

    override fun getCursorPosition(): PCMIterator = pcmIterator

    private fun translateIteratorPosition(iterator: PCMIterator): Long {
        val bytesPerSample = pcmHeader.getBitDepth() / 8
        val bytesPerFrame = bytesPerSample * pcmHeader.getChannelCount()
        val sampleRate = pcmHeader.getSampleRate()

        return pcmHeader.getHeaderSize() + when (iterator) {
            is PCMIterator.ByteIterator -> iterator.value
            is PCMIterator.MillisecondIterator -> (iterator.value * (sampleRate / 1000) * bytesPerFrame)
            is PCMIterator.FrameIterator -> iterator.value * bytesPerFrame
            is PCMIterator.SampleIterator -> iterator.value * bytesPerSample
            is PCMIterator.Begin -> 0
            is PCMIterator.End -> file.length() - pcmHeader.getHeaderSize()
            is PCMIterator.Invalid -> -45
        }
    }

    private fun calculateFrameSize(): Long {
        val bytesPerSample = pcmHeader.getBitDepth() / 8
        val frameSize = (bytesPerSample * pcmHeader.getSampleRate()).toLong()
        val availableBytes = (fileStream?.length() ?: 0L) - translateIteratorPosition(pcmIterator)
        return minOf(frameSize, availableBytes)
    }
}
