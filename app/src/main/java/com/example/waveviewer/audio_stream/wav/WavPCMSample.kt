import com.example.waveviewer.audio_stream.pcm.PCMSample

sealed class WavPCMSample : PCMSample {
    class Unsigned8BitSample(byte: Byte) : WavPCMSample() {
        // 8-bit WAV PCM is unsigned, centered at 128
        private val value = (byte.toInt() and 0xFF) - 128

        override fun getValue(): Int {
            return value
        }
    }

    class Signed16BitSample(b1: Byte, b2: Byte) : WavPCMSample() {
        private val value: Int

        init {
            // Little-endian: b1 is the least significant byte
            // b2 is the most significant byte (contains the sign bit)
            val unsigned = (b1.toInt() and 0xFF) or ((b2.toInt() and 0xFF) shl 8)

            // Properly handle signed 16-bit values
            value = if (unsigned >= 0x8000) {
                unsigned - 0x10000
            } else {
                unsigned
            }
        }

        override fun getValue(): Int {
            return value
        }
    }

    class Signed24BitSample(b1: Byte, b2: Byte, b3: Byte) : WavPCMSample() {
        private val value: Int

        init {
            // Little-endian: b1 is LSB, b3 is MSB (with sign bit)
            val unsigned = (b1.toInt() and 0xFF) or
                    ((b2.toInt() and 0xFF) shl 8) or
                    ((b3.toInt() and 0xFF) shl 16)

            // Properly handle signed 24-bit values
            value = if (unsigned >= 0x800000) {
                unsigned - 0x1000000
            } else {
                unsigned
            }
        }

        override fun getValue(): Int {
            return value
        }
    }
}