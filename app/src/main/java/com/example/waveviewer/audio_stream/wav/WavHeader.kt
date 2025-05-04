package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents a WAV audio file header.
 * Can both parse existing WAV headers and generate new ones.
 */
class WavHeader : PCMHeader {
    companion object {
        const val RIFF_IDENTIFIER = "RIFF"
        const val WAVE_FORMAT = "WAVE"
        const val FORMAT_IDENTIFIER = "fmt "
        const val DATA_IDENTIFIER = "data"
        const val PCM_FORMAT_CODE = 1
        const val MINIMUM_HEADER_SIZE = 44  // Standard 44-byte WAV header

        /**
         * Factory method that creates a new WAV header with the given parameters.
         *
         * @param channels Number of channels (1 for mono, 2 for stereo)
         * @param sampleRate Sample rate in Hz (e.g., 44100, 48000)
         * @param bitsPerSample Bits per sample (8, 16, 24, or 32)
         * @param dataSize Size of audio data in bytes
         * @return A new WavHeader instance with the specified parameters
         */
        fun create(channels: Int, sampleRate: Int, bitsPerSample: Int, dataSize: Int): WavHeader {
            // Validate parameters
            if (channels <= 0) {
                throw IllegalArgumentException("Channel count must be positive, got $channels")
            }
            if (sampleRate <= 0) {
                throw IllegalArgumentException("Sample rate must be positive, got $sampleRate")
            }
            if (bitsPerSample !in listOf(8, 16, 24, 32)) {
                throw IllegalArgumentException("Bits per sample must be 8, 16, 24, or 32, got $bitsPerSample")
            }
            if (dataSize < 0) {
                throw IllegalArgumentException("Data size must be non-negative, got $dataSize")
            }

            // Calculate derived values
            val blockAlign = channels * (bitsPerSample / 8)
            val byteRate = sampleRate * blockAlign
            val headerSize = MINIMUM_HEADER_SIZE
            val totalSize = headerSize + dataSize

            // Create a new byte array for the header
            val buffer = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)

            // Write the header
            buffer.put(RIFF_IDENTIFIER.toByteArray())
            buffer.putInt(totalSize - 8)  // Chunk size (file size - 8)
            buffer.put(WAVE_FORMAT.toByteArray())
            buffer.put(FORMAT_IDENTIFIER.toByteArray())
            buffer.putInt(16)  // Format chunk size (16 for PCM)
            buffer.putShort(PCM_FORMAT_CODE.toShort())  // Audio format (1 for PCM)
            buffer.putShort(channels.toShort())  // Number of channels
            buffer.putInt(sampleRate)  // Sample rate
            buffer.putInt(byteRate)  // Byte rate
            buffer.putShort(blockAlign.toShort())  // Block align
            buffer.putShort(bitsPerSample.toShort())  // Bits per sample
            buffer.put(DATA_IDENTIFIER.toByteArray())
            buffer.putInt(dataSize)  // Data size

            // Create and return the header
            return WavHeader(buffer.array())
        }
    }

    // Raw bytes of the header
    private val rawBytes: ByteArray

    // Standard WAV header fields with descriptive names
    private val riffChunkId: String
    private val chunkSize: Int
    private val waveFormat: String
    private val formatChunkId: String
    private val formatChunkSize: Int
    private val formatCode: Int
    private val channelCount: Int
    private val sampleRate: Int
    private val byteRate: Int
    private val blockAlign: Int
    private val bitsPerSample: Int
    private val dataChunkId: String
    private val dataSize: Int

    /**
     * Constructor that validates the provided byte array as a WAV header.
     *
     * @param byteArray The raw byte array to parse as a WAV header
     * @throws PCMError.InvalidHeader if the byte array does not contain a valid WAV header
     */
    constructor(byteArray: ByteArray) {
        if (byteArray.size < MINIMUM_HEADER_SIZE) {
            throw PCMError.InvalidHeader("WAV header is too small: ${byteArray.size} bytes (minimum $MINIMUM_HEADER_SIZE bytes required)")
        }

        try {
            rawBytes = byteArray.copyOf()

            // Parse the header
            riffChunkId = String(byteArray, 0, 4)
            chunkSize = readIntLE(byteArray, 4)
            waveFormat = String(byteArray, 8, 4)
            formatChunkId = String(byteArray, 12, 4)
            formatChunkSize = readIntLE(byteArray, 16)
            formatCode = readShortLE(byteArray, 20)
            channelCount = readShortLE(byteArray, 22)
            sampleRate = readIntLE(byteArray, 24)
            byteRate = readIntLE(byteArray, 28)
            blockAlign = readShortLE(byteArray, 32)
            bitsPerSample = readShortLE(byteArray, 34)
            dataChunkId = String(byteArray, 36, 4)
            dataSize = readIntLE(byteArray, 40)


        } catch (e: IndexOutOfBoundsException) {
            throw PCMError.InvalidHeader("Error parsing WAV header: ${e.message}")
        }
    }

    private fun readShortLE(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 2 > bytes.size) {
            throw IndexOutOfBoundsException("Cannot read short at offset $offset (buffer size: ${bytes.size})")
        }
        return ByteBuffer.wrap(bytes, offset, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short.toInt() and 0xFFFF
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) {
            throw IndexOutOfBoundsException("Cannot read int at offset $offset (buffer size: ${bytes.size})")
        }
        return ByteBuffer.wrap(bytes, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    override fun getBitDepth(): Int = bitsPerSample

    override fun getBitRate(): Int = byteRate * 8

    override fun getSampleRate(): Int = sampleRate

    override fun getChannelCount(): Int = channelCount

    override fun getHeaderSize(): Int = MINIMUM_HEADER_SIZE

    override fun getSampleCount(): Int = dataSize / (bitsPerSample / 8) / channelCount


    fun isValid(): Boolean {
        // Verify standard WAV format identifiers
        val isCorrectFormat = riffChunkId == RIFF_IDENTIFIER &&
                waveFormat == WAVE_FORMAT &&
                formatChunkId == FORMAT_IDENTIFIER &&
                dataChunkId == DATA_IDENTIFIER &&
                formatCode == PCM_FORMAT_CODE
        if (!isCorrectFormat) {
            return false
        }

        // Verify bit depth is supported
        if (bitsPerSample !in listOf(8, 16, 24, 32)) {
            return false
        }

        // Verify channel count is positive
        if (channelCount <= 0) {
            return false
        }

        // Verify sample rate is positive
        if (sampleRate <= 0) {
            return false
        }

        // Verify internal consistency of calculations
        val expectedByteRate = sampleRate * channelCount * (bitsPerSample / 8)
        val expectedBlockAlign = channelCount * (bitsPerSample / 8)

        return byteRate == expectedByteRate && blockAlign == expectedBlockAlign
    }

    fun getHeaderBytes(): ByteArray {
        return rawBytes.copyOf(MINIMUM_HEADER_SIZE)
    }


    override fun toString(): String {
        return """WAV Header:
            |  RIFF ID: $riffChunkId
            |  Chunk Size: $chunkSize bytes
            |  Format: $waveFormat
            |  Format Chunk ID: $formatChunkId
            |  Format Chunk Size: $formatChunkSize bytes
            |  Format Code: $formatCode
            |  Channels: $channelCount
            |  Sample Rate: $sampleRate Hz
            |  Byte Rate: $byteRate bytes/sec
            |  Block Align: $blockAlign bytes
            |  Bits Per Sample: $bitsPerSample bits
            |  Data Chunk ID: $dataChunkId
            |  Data Size: $dataSize bytes
            |  Valid: ${isValid()}
        """.trimMargin()
    }
}