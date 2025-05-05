package com.example.waveviewer.view.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.Default.UrlSafe
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

class HomeViewModel : ViewModel() {

    private val _recentFileHeaders = MutableStateFlow(emptyList<FileHeader>())
    val recentFileHeader = _recentFileHeaders.asStateFlow()

    private val _assetFileHeaders = MutableStateFlow(emptyList<FileHeader>())
    val assetFileHeader = _assetFileHeaders.asStateFlow()


    private val assetFiles = listOf(
        "gravitational_wave_mono_44100Hz_16bit.wav",
        "stereo.wav",
        "whistle_mono_44100Hz_16bit.wav",
        "music_mono_44100Hz_16bit.wav"
    )

    init {
        val recentFiles = loadRecentFiles().map{
            createFileHeader(it , it.name)
        }
        _recentFileHeaders.update { recentFiles }
    }
    fun setAssetHeaders(getFileForAsset : (String)->File){
       val assetFileHandles = assetFiles.associate{
           Pair(it , getFileForAsset(it))
       }

       val assetHeaders = assetFileHandles.map{
         createFileHeader(it.value , it.key)
       }

        _assetFileHeaders.update { assetHeaders }
    }

    private fun loadRecentFiles() : List<File>{
     return emptyList()
    }

    private fun createFileHeader(file : File , label: String) : FileHeader{
        return processFileHeader(file , label)
    }
    @OptIn(ExperimentalEncodingApi::class)
    private fun processFileHeader(fileHandle: File , label:String): FileHeader {
        val byteSize = fileHandle.length() - 44
        val mbSize = byteSize / (1024 * 1024).toFloat()
        val (minuteCount, secondCount) = calculateDuration(byteSize)

        // Create a FileHeader for each audio file
        return FileHeader(
            uid = UUID.randomUUID().hashCode(),
            fileName = fileHandle.name,
            mbSizeSt = formatSize(mbSize),
            label = label,
            encodedPath = UrlSafe.encode(
                fileHandle.path.encodeToByteArray(),
            ),
            durationSt = formatDuration(minuteCount, secondCount)
        )
    }


    /**
     * Calculates the duration of the audio file based on its size in bytes.
     * This method estimates the duration by using the known bytes per second for WAV files.
     *
     * @param byteSize The size of the file in bytes.
     * @return A [Pair] of minutes and seconds that represents the duration of the file.
     */
    private fun calculateDuration(byteSize: Long): Pair<Float, Float> {
        val bytesPerSecond = 44100 * 2 // 2 bytes per sample

        // Total seconds as float
        val totalSeconds = byteSize.toFloat() / bytesPerSecond

        // Extract minutes and remaining seconds
        val minuteCount = (totalSeconds / 60)
        val secondCount = (totalSeconds % 60)

        return minuteCount to secondCount
    }

    /**
     * Formats the size of a file into a human-readable string, either in KB or MB.
     *
     * @param mbSize The size of the file in MB.
     * @return A formatted string representing the file size (e.g., "5MB" or "500KB").
     */
    private fun formatSize(mbSize: Float): String {
        return if (mbSize < 1f) {
            "${(mbSize * 1000).toInt()} Kbs"
        } else {
            String.format("%.2f Mbs", mbSize)
        }
    }

    /**
     * Formats the duration of a file into a human-readable string.
     *
     * @param minuteCount The number of minutes in the file's duration.
     * @param secondCount The number of seconds in the file's duration.
     * @return A formatted string representing the file's duration (e.g., "2m : 30s" or "45s").
     */
    private fun formatDuration(minuteCount: Float, secondCount: Float): String {
        return if (minuteCount < 1) {
            "${String.format("%.2f", secondCount)}s"
        } else {
            "${minuteCount.toInt()}m : ${secondCount.roundToInt()}s"
        }
    }

}