package com.example.waveviewer.audio_stream.pcm

sealed class PCMError(message: String) : Exception(message) {
    class InvalidBitDepth(message: String) : PCMError(message)
    class FileStreamError(message: String) : PCMError(message)
    class InvalidHeader(message: String) : PCMError(message)
    class InvalidIterator(message: String) : PCMError(message)
    class UnknownError(message: String) : PCMError(message)
}