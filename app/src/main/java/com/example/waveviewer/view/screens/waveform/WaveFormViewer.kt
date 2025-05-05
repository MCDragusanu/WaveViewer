package com.example.waveviewer.view.screens.waveform

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.waveviewer.music_player.LiveFeedClock
import com.example.waveviewer.view.waveform.ProgressBar
import com.example.waveviewer.view.waveform.UniversalWaveViewer
import com.example.waveviewer.view.waveform.formatDuration

@Composable
fun WaveForm(onBackIsPressed : ()->Unit , viewModel: WaveFormViewModel) {

    val stream by viewModel.audioStream.collectAsState()
    val songLength by viewModel.mediaLength.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("WaveForm", "ON_PAUSE - releasing resources")
                    viewModel.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d("WaveForm", "ON_DESTROY - clearing resources")
                    viewModel.stop()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept system back button
    BackHandler {
        Log.d("WaveForm", "Back pressed from WaveForm")
        viewModel.stop()
        onBackIsPressed()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(onBackIsPressed = onBackIsPressed, title = "Wave Viewer")
            AnimatedVisibility(stream == null) {
                LoadingIndicator()
            }
            AnimatedVisibility(stream != null) {

                AudioPlayer(Modifier, viewModel)
            }
        }
    }
}
@Composable
fun AudioPlayer(
    modifier: Modifier = Modifier,
    viewModel: WaveFormViewModel,

) {
    val stream by viewModel.audioStream.collectAsState()
    val songLength by viewModel.mediaLength.collectAsState()
    val playerState by viewModel.mediaPlayerState.collectAsState()
    val progressFlow = viewModel.progressPercent
    val progress by progressFlow.collectAsState()


    // Temporary progress value during drag
    var dragProgress by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // Waveform visualization
        AnimatedVisibility(
            visible = stream != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            stream?.let {
                UniversalWaveViewer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    totalFrames = 50,
                    visibleFrameCount = 50,
                    songDurationInMs = songLength.toInt(),
                    stream = it,
                    progressFlow = progressFlow,

                )
            }
        }

        // Animated loading or error state
        AnimatedVisibility(
            visible = stream == null || playerState == LiveFeedClock.ClockState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (playerState == LiveFeedClock.ClockState.Error) {
                Text(
                    text = "Error loading audio",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Loading audio...",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio controls
        PlayerControls(
            playerState = playerState,
            progress = progress,
            duration = songLength,
            onPlayPause = {
                if (playerState == LiveFeedClock.ClockState.Playing) {
                    viewModel.pause()
                } else {
                    viewModel.start()
                }
            },
            onStop = { viewModel.stop() },
            onReset = {
                viewModel.stop()
                viewModel.setProgress(0f)
                viewModel.start()
            },
            onProgressChange = { newProgress ->
                viewModel.pause()
                dragProgress = newProgress
                viewModel.setProgress(dragProgress)
            },
            onProgressChangeFinished = {
                // When user finishes dragging the slider


            }
        )
    }
}

@Composable
fun PlayerControls(
    playerState: LiveFeedClock.ClockState,
    progress: Float,
    duration: Long,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Current time
            Text(
                text = formatDuration(progress * duration),
                style = MaterialTheme.typography.bodyMedium
            )

            // Total duration
            Text(
                text = formatDuration(duration.toFloat()),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Progress slider
        ProgressBar(

            currentProgress = { progress },
          onProgressChanged = {onProgressChange(it)},
            modifier = Modifier.fillMaxWidth()
        )

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {


            // Stop button
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (playerState == LiveFeedClock.ClockState.Playing)
                        Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = if (playerState == LiveFeedClock.ClockState.Playing)
                        "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Playback state indicator
        Text(
            text = when (playerState) {
                LiveFeedClock.ClockState.Playing -> "Playing"
                LiveFeedClock.ClockState.Paused -> "Paused"
                LiveFeedClock.ClockState.Stopped -> "Stopped"
                LiveFeedClock.ClockState.Completed -> "Completed"
                LiveFeedClock.ClockState.Error -> "Error"
                else -> "Ready"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (playerState) {
                LiveFeedClock.ClockState.Error -> MaterialTheme.colorScheme.error
                LiveFeedClock.ClockState.Playing -> Color(0xFF4CAF50) // Green
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
@Composable
private fun Header(onBackIsPressed: () -> Unit, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackIsPressed) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go back"
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun LoadingIndicator() {
    // Simple loading indicator - can be replaced with a custom animation
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Could use a CircularProgressIndicator or custom animation here
        Text(
            text = "Loading audio stream...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ErrorScreen(
    onBackIsPressed: () -> Unit,
    title: String = "Error Occurred",
    subtitle: String = "Something went wrong while processing the audio",
    body: String = "Please try again or select a different audio file",
    errorIcon: ImageVector = Icons.Filled.Warning,
    thumbnailResId: Int? = null
) {
    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(onBackIsPressed = onBackIsPressed, title = title)

            Spacer(modifier = Modifier.height(32.dp))

            // Error icon or thumbnail
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailResId != null) {
                    Image(
                        painter = painterResource(id = thumbnailResId),
                        contentDescription = "Error thumbnail",
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = errorIcon,
                        contentDescription = "Error icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error information
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Retry button
            Button(
                onClick = onBackIsPressed,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Go Back")
            }
        }
    }
}