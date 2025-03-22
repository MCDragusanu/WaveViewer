package com.example.waveviewer.audio_stream.pcm

import PCMIterator
import android.util.Range

abstract class  PCMInputStream : Collection<PCMFrame> {

    abstract fun open()
    abstract fun close()

   abstract fun getDescriptor() : PCMHeader

   abstract fun readNextFrame(sampleCount : Int) : PCMFrame?

   abstract fun getRange(range: Range<Int> , sampleCountPerFrame : Int = 44100) : Array<PCMFrame>

    fun use( action : (PCMInputStream)->Unit){
        open()
        action(this)
        close()
    }

    abstract fun getTotalFrameCount(frameSampleCount: Int): Int

    abstract fun isOpen() : Boolean

    abstract fun setProgress(progress: Float)

}