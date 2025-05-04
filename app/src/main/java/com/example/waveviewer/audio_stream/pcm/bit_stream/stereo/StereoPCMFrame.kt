package com.example.waveviewer.audio_stream.pcm.bit_stream.stereo

import com.example.waveviewer.audio_stream.pcm.bit_stream.mono.MonoPCMFrame

class StereoPCMFrame(val leftChannel : MonoPCMFrame, val rightChannel : MonoPCMFrame)