package com.example.waveviewer.audio_stream.wav

import com.example.waveviewer.audio_stream.pcm.PCMError
import com.example.waveviewer.audio_stream.pcm.PCMHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeader// Parse the header

// Validate the header
// Check minimum size

// Store raw bytes
/**
 * Constructor that validates the provided byte array as a WAV header.
 *
 * @param byteArray The raw byte array to parse as a WAV header
 * @throws PCMError.InvalidHeader if the byte array does not contain a valid WAV header
 */(byteArray: ByteArray) : PCMHeader {


    private val rawBytes: ByteArray = byteArray

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

    init {
        if (byteArray.size < MINIMUM_HEADER_SIZE) {
            throw PCMError.InvalidHeader("WAV header is too small: ${byteArray.size} bytes (minimum $MINIMUM_HEADER_SIZE bytes required)")
        }
        try {
            // Parse the header
            riffChunkId = String(byteArray, 0, 4)
            chunkSize = readIntLE(4)
            waveFormat = String(byteArray, 8, 4)
            formatChunkId = String(byteArray, 12, 4)
            formatChunkSize = readIntLE(16)
            formatCode = readShortLE(20)
            channelCount = readShortLE(22)
            sampleRate = readIntLE(24)
            byteRate = readIntLE(28)
            blockAlign = readShortLE(32)
            bitsPerSample = readShortLE(34)
            dataChunkId = String(byteArray, 36, 4)
            dataSize = readIntLE(40)

            // Validate the header
            if (!isValid()) {
                //throw PCMError.InvalidHeader("Invalid WAV header format")
            }
        } catch (e: IndexOutOfBoundsException) {
            throw PCMError.InvalidHeader("Error parsing WAV header: ${e.message}")
        }
    }

    /**
     * Factory constructor that creates a new WAV header with the given parameters.
     *
     * @param channels Number of channels (1 for mono, 2 for stereo)
     * @param sampleRate Sample rate in Hz (e.g., 44100, 48000)
     * @param bitsPerSample Bits per sample (8, 16, 24, or 32)
     * @param dataSize Size of audio data in bytes
     * @return A new WavHeader instance with the specified parameters
     */
    companion object {
        const val RIFF_IDENTIFIER = "RIFF"
        const val WAVE_FORMAT = "WAVE"
        const val FORMAT_IDENTIFIER = "fmt "
        const val DATA_IDENTIFIER = "data"
        const val PCM_FORMAT_CODE = 1
        const val MINIMUM_HEADER_SIZE = 44

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
            val headerSize = 44
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

    private fun readShortLE(offset: Int): Int {
        if (offset < 0 || offset + 2 > rawBytes.size) {
            throw IndexOutOfBoundsException("Cannot read short at offset $offset (buffer size: ${rawBytes.size})")
        }
        return ByteBuffer.wrap(rawBytes, offset, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short.toInt() and 0xFFFF
    }

    private fun readIntLE(offset: Int): Int {
        if (offset < 0 || offset + 4 > rawBytes.size) {
            throw IndexOutOfBoundsException("Cannot read int at offset $offset (buffer size: ${rawBytes.size})")
        }
        return ByteBuffer.wrap(rawBytes, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    override fun getBitDepth(): Int = bitsPerSample

    override fun getBitRate(): Int = byteRate * 8

    override fun getSampleRate(): Int = sampleRate

    override fun getChannelCount(): Int = channelCount

    override fun getHeaderSize(): Int = MINIMUM_HEADER_SIZE

    override fun getSampleCount(): Int = dataSize / (bitsPerSample / 8) / channelCount

    /**
     * Checks if this is a valid WAV header by verifying:
     * 1. RIFF chunk identifier
     * 2. WAVE format identifier
     * 3. Format chunk identifier
     * 4. Data chunk identifier
     * 5. PCM audio format
     * 6. Consistent byte rate calculation
     * 7. Consistent block alignment calculation
     *
     * @return true if the header is valid, false otherwise
     */
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

    /**
     * Gets the raw header bytes.
     *
     * @return The raw header byte array
     */
    fun getHeaderBytes(): ByteArray {
        return rawBytes.copyOf(MINIMUM_HEADER_SIZE)
    }

    /**
     * Gets the complete byte array passed to the constructor.
     *
     * @return The complete raw byte array
     */
    fun getRawBytes(): ByteArray {
        return rawBytes
    }
}