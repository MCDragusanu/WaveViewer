package com.example.waveviewer.audio_stream.wav

import PCMIterator
import android.util.Log
import android.util.Range
import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.PCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.min

class WavInputStream(private val file: File, private val samplesPerFrame : Int = 44100,

) : PCMInputStream() {

    override val size: Int = file.length().toInt()

    override fun contains(element: PCMFrame): Boolean {
        return firstOrNull { it.hashCode() == element.hashCode() }!=null
    }

    override fun containsAll(elements: Collection<PCMFrame>): Boolean {
       return elements.count { !contains(it) } == 0
    }

    override fun isEmpty(): Boolean {
        return size <= pcmHeader.getHeaderSize()
    }

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

    override fun getRange(range: Range<Int>, sampleCountPerFrame: Int ): Array<PCMFrame> {
        resetReading()

        if (pcmHeader.getChannelCount() > 1) {
            TODO("Implement multi-channel support")
        }

        try {
            byteOffset += range.lower * sampleCountPerFrame
            val list = arrayListOf<PCMFrame>()

            this.use {
                for(i in range.lower until range.upper){
                    val frame = readNextFrame(sampleCountPerFrame) ?: break
                    list.add(frame)
                }
            }
            resetReading()
            return list.toTypedArray()

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyArray()
        }
    }

    override fun getTotalFrameCount(frameSampleCount: Int): Int {
        val totalSize = file.length()
        val totalSampleByteSize = totalSize - pcmHeader.getHeaderSize()
        val frameByteSize = frameSampleCount * (pcmHeader.getBitDepth() /8)
        return (totalSampleByteSize / frameByteSize).toInt()
    }

    override fun isOpen() : Boolean{
        return this.fileStream!=null
    }

    override fun setProgress(progress: Float) {
        try {


            // Ensure progress is within valid range
            val clampedProgress = progress.coerceIn(0f, 1f)

            // Calculate the exact byte offset based on progress
            val totalSampleByteSize = size - pcmHeader.getHeaderSize()
            val bytePosition = (clampedProgress * totalSampleByteSize).toInt()

            // Align to the nearest sample (to avoid reading partial samples)
            val bytesPerSample = pcmHeader.getBitDepth() / 8

            val totalSamples = (totalSampleByteSize) / bytesPerSample
            val durationMs= ((totalSamples / pcmHeader.getSampleRate()) * 1000) * progress

            val alignedByteOffset = (bytePosition / bytesPerSample) * bytesPerSample

            // Set the new byte offset, making sure we don’t seek before header
            byteOffset = pcmHeader.getHeaderSize() + alignedByteOffset

            Log.d("Test", "WAV Stream pos : ${durationMs} ms")

            // Seek the file stream to the new position
            fileStream?.seek(byteOffset.toLong())

        } catch (e: Exception) {
            Log.e("Test", "Error seeking: ${e.message}")
        }
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

    private fun resetReading(){
        byteOffset = pcmHeader.getHeaderSize()
        currentFrameIndex = 0

    }
    override fun iterator(): Iterator<PCMFrame> {
        // Reset file stream to the beginning of the data section
        resetReading()
        open()
        val firstFrame = readNextFrame(samplesPerFrame) ?: return emptyList<PCMFrame>().iterator()

        return PCMIterator.PCMFrameIterator(this, firstFrame, currentFrameIndex, frameCount)
    }


}
