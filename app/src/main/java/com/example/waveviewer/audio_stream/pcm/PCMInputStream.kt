package com.example.waveviewer.audio_stream.pcm

import PCMIterator

abstract class  PCMInputStream : Iterable<PCMFrame> {

    abstract fun open()
    abstract fun close()

   abstract fun getDescriptor() : PCMHeader

   abstract fun readNextFrame(sampleCount : Int) : PCMFrame?


    fun use( action : (PCMInputStream)->Unit){
        open()
        action(this)
        close()
    }



}