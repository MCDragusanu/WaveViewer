package com.example.waveviewer.audio_stream.pcm.stereo

import com.example.waveviewer.audio_stream.pcm.mono.MonoPCMFrame

class StereoPCMFrame(val leftChannel : MonoPCMFrame, val rightChannel : MonoPCMFrame)