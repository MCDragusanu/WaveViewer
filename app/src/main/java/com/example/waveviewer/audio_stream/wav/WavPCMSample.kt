package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMSample

sealed class WavPCMSample {
    class Unsigned8BitSample(byte: Byte) : PCMSample {
        private val value = byte.toInt() and 0xFF

        override fun getValue(): Int {
            return value
        }
    }

    class Signed16BitSample(b1: Byte, b2: Byte) : PCMSample {
        private val value: Int

        init {
            val b11 = b1.toInt() and 0xFF
            val b22 = b2.toInt() shl 8
            value = (b11 or b22).toShort().toInt()
        }

        override fun getValue(): Int {
            return value
        }
    }

    class Signed24BitSample(b1: Byte, b2: Byte, b3: Byte) : PCMSample {
        private val value: Int

        init {
            val b11 = b1.toInt() and 0xFF
            val b22 = b2.toInt() and 0xFF shl 8
            val b33 = b3.toInt() shl 16
            value = (b11 or b22 or b33) or (if (b33 and 0x800000 != 0) 0xFF000000.toInt() else 0)
        }

        override fun getValue(): Int {
            return value
        }
    }
}
