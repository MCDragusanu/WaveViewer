package com.example.waveviewer.audio_stream.pcm.mono

import com.example.waveviewer.audio_stream.pcm.PCMSample

interface MonoPCMFrame : Collection<PCMSample> {

    fun getSampleByteStride() : Int
    fun getChannelCount() : Int
    fun getSampleCapacity() : Int

    fun getBytes() : ByteArray
    fun get(index : Int) : PCMSample


}