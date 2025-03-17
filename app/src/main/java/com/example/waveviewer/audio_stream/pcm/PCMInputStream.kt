package com.example.waveviewer.audio_stream.pcm

abstract class  PCMInputStream {

   protected abstract fun open()
   protected abstract fun close()

   abstract fun getDescriptor() : PCMHeader

   abstract fun readNextFrame() : PCMFrame?
   abstract fun readNextFrame(pcmIterator: PCMIterator) : PCMFrame?

   abstract fun moveCursor(pcmIterator: PCMIterator)
   abstract fun getCursorPosition() : PCMIterator

    fun use( action : (PCMInputStream)->Unit){
        open()
        action(this)
        close()
    }


}