package com.example.waveviewer.audio_stream.pcm

interface PCMFrame : Collection<PCMSample> {

    fun getSampleByteStride() : Int
    fun getChannelCount() : Int
    fun getSampleCapacity() : Int

    fun getBytes() : ByteArray
    fun get(index : Int) : PCMSample


}