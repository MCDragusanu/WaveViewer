package com.example.waveviewer.audio_stream.pcm

interface PCMFrame {
    fun getSamples() : ByteArray
    fun getSampleCount() : Int
    operator fun get(index : Int) : PCMSample
    fun getSampleStride() : Int
    fun getChannelCount() : Int
    fun getSampleCapacity() : Int
}