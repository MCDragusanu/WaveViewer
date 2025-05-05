package com.example.waveviewer.view.navigation


import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.waveviewer.view.screens.home.HomeScreen
import com.example.waveviewer.view.screens.home.HomeViewModel
import kotlinx.serialization.json.Json
import java.io.File


object MainNavGraph {

    /**
     * - [SplashScreen] composable is shown when the app is first launched.
     * - After the splash screen completes, the app navigates to the [HomeScreen].
     * - On the [HomeScreen], users can interact with the list of recent files and navigate to the [WaveViewerScreen].
     * - The [WaveViewerScreen] is displayed to view the waveform of a selected audio file.
     *
     */
    @Composable
     fun Main(obtainFileForAsset : (String) -> File) {
        // Initialize the NavController
        val navController = rememberNavController()

        // Set up the NavHost with the start route as SplashScreen
        NavHost(navController, "home") {

            // SplashScreen composable
            composable("home") {
                val homeViewModel = viewModel<HomeViewModel>()
                // Show splash screen and navigate to HomeScreen on completion
                HomeScreen(onBackIsPressed = {
                    navController.popBackStack()
                }, obtainFileForAsset = obtainFileForAsset, viewModel = homeViewModel) {
                    Log.d("Test" , "On Open File invoked : ${it}")
                }
            }

        }
    }
}
