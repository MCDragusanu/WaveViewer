package com.example.waveviewer.content_provider

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.util.UUID

class ContentResolverAdapter constructor(private val context: () -> Context) {
    private val contentResolver = context().contentResolver
    private val TAG = "ContentResolverAdapter"

    /**
     * Gets a FileDescriptor from a URI. This is what you need to use with your PCMStreamWrapper.
     * This method works properly with Storage Access Framework URIs.
     */
    fun getFileDescriptor(uri: Uri): FileDescriptor? {
        return try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            parcelFileDescriptor?.fileDescriptor
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file descriptor: ${e.message}")
            null
        }
    }

    /**
     * Creates a temp file from the URI content
     * Fixed to work with content:// URIs
     */
    fun readIntoTempFile(uri: Uri , file: File): File? {


        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            if (file.exists()) file else null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file: ${e.message}")
            null
        }
    }

    /**
     * Gets file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        // Try using OpenableColumns which works with content:// URIs
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return cursor.getString(displayNameIndex)
                }
            }
        }

        // Fallback to last path segment
        return uri.lastPathSegment
    }
}