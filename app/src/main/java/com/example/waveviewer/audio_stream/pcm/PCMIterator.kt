package com.example.waveviewer.audio_stream.pcm

sealed class PCMIterator (val value : Long , val step : Int) {

    class ByteIterator(byteOffset : Long) : PCMIterator(byteOffset , 1)
    class MillisecondIterator(msOffset : Long, stepInBytes : Int) : PCMIterator(msOffset , stepInBytes)
    class FrameIterator(frameOffset : Long, stepInBytes : Int)  : PCMIterator(frameOffset, stepInBytes)
    class SampleIterator(sampleOffset : Long, stepInBytes : Int) : PCMIterator(sampleOffset , stepInBytes)

    data object Begin : PCMIterator(0 , 0)
    data object End : PCMIterator(-1 , -1)
    data object Invalid : PCMIterator(-1 , 0)
}