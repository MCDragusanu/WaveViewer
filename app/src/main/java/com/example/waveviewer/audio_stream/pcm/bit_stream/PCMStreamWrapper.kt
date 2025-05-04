package com.example.waveviewer.audio_stream.pcm.bit_stream

import com.example.waveviewer.audio_stream.pcm.PCMHeader
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMFrame
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMStream
import com.example.waveviewer.audio_stream.pcm.bit_stream.stereo.StereoPCMFrame
import com.example.waveviewer.audio_stream.pcm.bit_stream.stereo.StereoPCMStream

sealed class PCMStreamWrapper {
    abstract fun open()
    abstract fun close()
    abstract fun isOpen(): Boolean
    abstract fun setProgress(progress: Float)
    abstract fun getDescriptor(): PCMHeader

    data class Mono(val stream: MonoPCMStream) : PCMStreamWrapper() {
        override fun open() = stream.open()
        override fun close() = stream.close()
        override fun isOpen() = stream.isOpen()
        override fun setProgress(progress: Float) = stream.setProgress(progress)
        override fun getDescriptor() = stream.getDescriptor()
        fun readNextFrame(sampleCount: Int): MonoPCMFrame? = stream.readNextFrame(sampleCount)
    }

    data class Stereo(val stream: StereoPCMStream) : PCMStreamWrapper() {
        override fun open() = stream.open()
        override fun close() = stream.close()
        override fun isOpen() = stream.isOpen()
        override fun setProgress(progress: Float) = stream.setProgress(progress)
        override fun getDescriptor() = stream.getDescriptor()
        fun readNextFrame(sampleCount: Int): StereoPCMFrame? = stream.readNextFrame(sampleCount)
    }
}