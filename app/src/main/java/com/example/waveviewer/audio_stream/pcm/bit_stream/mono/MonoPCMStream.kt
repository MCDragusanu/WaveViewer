package com.example.waveviewer.audio_stream.pcm.bit_stream.mono

import android.util.Range
import com.example.waveviewer.audio_stream.pcm.PCMHeader

abstract class  MonoPCMStream{

    abstract fun open()
    abstract fun close()

   abstract fun getDescriptor() : PCMHeader

   abstract fun readNextFrame(sampleCount : Int) : MonoPCMFrame?

    fun use( action : (MonoPCMStream)->Unit){
        open()
        action(this)
        close()
    }
    abstract fun isOpen() : Boolean

    abstract fun setProgress(progress: Float)

}