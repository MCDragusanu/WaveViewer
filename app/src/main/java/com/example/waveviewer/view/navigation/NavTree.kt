package com.example.waveviewer.view.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.waveviewer.content_provider.ContentResolverAdapter
import com.example.waveviewer.music_player.MediaPlayerAdapter
import com.example.waveviewer.view.screens.home.HomeScreen
import com.example.waveviewer.view.screens.home.HomeViewModel
import com.example.waveviewer.view.screens.waveform.ErrorScreen
import com.example.waveviewer.view.screens.waveform.WaveForm
import com.example.waveviewer.view.screens.waveform.WaveFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MainNavGraph {

    @Composable
    fun Main(contentResolverAdapter: ContentResolverAdapter, obtainFileForAsset: (String) -> File) {
        val navController = rememberNavController()

        NavHost(navController, startDestination = "home") {

            composable("home") {
                val homeViewModel = viewModel<HomeViewModel>()
                HomeScreen(
                    onBackIsPressed = { navController.popBackStack() },
                    obtainFileForAsset = obtainFileForAsset,
                    viewModel = homeViewModel
                ) { fileUri ->
                    Log.d("Test", "On Open File invoked: $fileUri")
                    navController.navigate("waveform/${Uri.encode (fileUri)}")
                }
            }
            composable("error") {
                ErrorScreen(onBackIsPressed = {
                    navController.popBackStack()
                    navController.navigate("home")
                })
            }

            composable(
                "waveform/{fileUri}",
                arguments = listOf(navArgument("fileUri") { type = NavType.StringType })
            ) { backStackEntry ->
                Log.d("Test" , backStackEntry.arguments?.size().toString())
                Log.d("Test" ,"File Uri found : " +  backStackEntry.arguments?.getString("fileUri").toString())
                val uri = backStackEntry.arguments?.getString("fileUri")
                if (uri.isNullOrBlank()) {
                    Log.d("Test" , "No Uri Found!")
                    navController.popBackStack()
                    navController.navigate("error")
                } else {
                    val viewModel = viewModel<WaveFormViewModel>()
                    val mediaPlayerAdapter = MediaPlayerAdapter()
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        viewModel.setContentResolver(contentResolverAdapter)
                        viewModel.setMediaPlayer(mediaPlayerAdapter)
                        val decodedUri = Uri.decode(uri)
                        val foundUri = Uri.parse(decodedUri)
                        Log.d("Test" , "Uri : ${foundUri.toString()}")
                        viewModel.setUri(foundUri, context = context) {
                            Log.d("Test" , "Failed to load Uri!")
                            navController.popBackStack()
                            navController.navigate("error")
                        }
                    }

                    WaveForm(
                        onBackIsPressed = {
                            navController.popBackStack()
                            navController.navigate("home")
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
