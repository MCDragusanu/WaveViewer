package com.example.waveviewer.audio_stream.pcm.bit_stream.stereo

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.bit_stream.PCMStreamWrapper
import com.example.waveviewer.audio_stream.wav.WavHeader
import com.example.waveviewer.audio_stream.wav_mono.MonoWavStream
import com.example.waveviewer.audio_stream.wav_stereo.StereoWavStream
import kotlinx.coroutines.internal.synchronized
import java.io.File

class PCMStreamFactory {
    companion object {
        @Volatile
        private var _instance: PCMStreamFactory? = null

        fun getInstance(): PCMStreamFactory {
            return _instance ?: kotlin.synchronized(this) {
                _instance ?: PCMStreamFactory().also { _instance = it }
            }
        }
    }

    fun provideBitStream(file: File): PCMStreamWrapper {
        // Validate file existence and extension
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        require(file.isFile) { "Path is not a file: ${file.absolutePath}" }
        require(file.extension.lowercase() in listOf("wav", "wave")) {
            "File is not a WAV file: ${file.absolutePath}"
        }

        // Read and parse the header
        val headerBuffer = ByteArray(WavHeader.MINIMUM_HEADER_SIZE)
        file.inputStream().use { stream ->
            val bytesRead = stream.read(headerBuffer)
            if (bytesRead < WavHeader.MINIMUM_HEADER_SIZE) {
                throw PCMError.InvalidHeader("File too small to contain a complete WAV header")
            }
        }

        // Create and validate the header
        val processedHeader = WavHeader(headerBuffer)


        // Create the appropriate stream wrapper based on channel count
        return when (processedHeader.getChannelCount()) {
            1 -> PCMStreamWrapper.Mono(MonoWavStream(file))
            2 -> PCMStreamWrapper.Stereo(StereoWavStream(file))
            else -> throw PCMError.UnknownError("Channel count ${processedHeader.getChannelCount()} not supported")
        }
    }
}