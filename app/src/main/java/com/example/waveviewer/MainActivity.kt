package com.example.waveviewer

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.waveviewer.content_provider.ContentResolverAdapter
import com.example.waveviewer.ui.theme.WaveViewerTheme
import com.example.waveviewer.view.navigation.MainNavGraph.Main

import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity()  {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contentProvider = ContentResolverAdapter{
            this
        }

        setContent {
            WaveViewerTheme {

                Main(contentResolverAdapter = contentProvider){ file->
                    this.getFileFromAssetFd(this , file)
                }
            }
        }
    }



    private fun getFileFromAssetFd(context: Context, assetName: String): File {
        return try {


            // Prepare the output directory
            val outputDir = File(context.dataDir, "assets")
            if (!outputDir.exists()) {
                val dirCreated = outputDir.mkdirs()
                if (!dirCreated) {
                    Log.e("AssetExtraction", "Failed to create directory: ${outputDir.path}")
                    return File("NoName")
                }
            }

            // Create the output file
            val outputFile = File(outputDir, assetName)
            if (outputFile.exists()) {
                return outputFile // File already exists, just return it
            }

            // Extract the asset
            context.assets.openFd(assetName).use { afd ->
                afd.createInputStream().use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    if(!outputFile.exists()) {
                        outputFile.createNewFile()
                    }
                }
            }

            outputFile
        } catch (e: IOException) {
            Log.e("AssetExtraction", "Failed to extract asset: $assetName", e)
            File("No")
        }
    }



}
