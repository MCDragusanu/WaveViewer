package com.example.waveviewer.audio_stream.pcm

abstract class PCMOutputStream {

    protected abstract fun open()
    protected abstract fun close()

    fun use(action : (PCMOutputStream)->Unit){
        open()
        action(this)
        close()
    }

   abstract fun appendFrame(frame : PCMFrame)
   abstract fun appendFrames(frames : Iterable<PCMFrame>)

   abstract fun clearAll()

}