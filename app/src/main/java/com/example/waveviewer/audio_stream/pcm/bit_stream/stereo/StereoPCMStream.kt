package com.example.waveviewer.audio_stream.pcm.bit_stream.stereo

import android.util.Range
import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMFrame
import com.example.waveviewer.audio_stream.pcm.PCMHeader

interface StereoPCMStream {

     fun open()

     fun close()

     fun getDescriptor() : PCMHeader

     fun readNextFrame(sampleCount : Int) : StereoPCMFrame?

    fun use( action : (StereoPCMStream)->Unit){
        open()
        action(this)
        close()
    }


     fun isOpen() : Boolean

     fun setProgress(progress: Float)
}