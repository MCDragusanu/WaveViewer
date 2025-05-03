package com.example.waveviewer.audio_processing

import com.example.waveviewer.audio_stream.pcm.mono.MonoPCMFrame
import kotlin.math.sqrt

class RMSCompute {
    companion object{
        fun computeRMS(frame : MonoPCMFrame, sampleCount : Int = 44100) : Array<Double>{
            val processed = frame.chunked(sampleCount).map { slice ->
                var s = 0.0
                for (pcmSample in slice) {
                    s+= pcmSample.getValue() * pcmSample.getValue()
                }
                val ms = s / slice.size
                val rms = sqrt(ms)
                rms
            }.toTypedArray()
            return processed
        }

    }
}