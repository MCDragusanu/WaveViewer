package com.example.waveviewer.audio_stream.pcm

interface PCMHeader {

    fun getBitDepth() : Int

    fun getBitRate() : Int

    fun getSampleRate() : Int

    fun getChannelCount() : Int

    fun getHeaderSize() : Int

    fun getSampleCount() : Int
}