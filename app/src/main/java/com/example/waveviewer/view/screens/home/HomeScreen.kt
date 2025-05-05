package com.example.waveviewer.view.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.waveviewer.R
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URI
import kotlin.random.Random

@Serializable
data class FileHeader(
    val uid: Int,
    val fileUri: String,
    val label: String,
    val fileName: String,
    val mbSizeSt: String,
    val durationSt: String
)

// A more curated and less saturated color palette
val modernColorPalette = listOf(
    Pair(Color(0xFFF5F7FA), Color(0xFFE4E8F0)), // Light gray gradient
    Pair(Color(0xFFE6F4F1), Color(0xFFD7EAE4)), // Mint green
    Pair(Color(0xFFF6F0F9), Color(0xFFEBDDF0)), // Lavender
    Pair(Color(0xFFF5F5F5), Color(0xFFE0E0E0)), // Silver
    Pair(Color(0xFFEFF6FF), Color(0xFFDCEAFE))  // Sky blue
)

@Composable
fun HomeScreen(
    onBackIsPressed: () -> Unit,
    obtainFileForAsset: (String) -> File,
    viewModel: HomeViewModel,
    onOpenFile: (String) -> Unit
) {
    val backgroundColor = Color(0xFFFAFAFC)

    LaunchedEffect(Unit) {
        viewModel.setAssetHeaders(obtainFileForAsset)
    }
    var fileUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    val assetFiles by viewModel.assetFileHeader.collectAsState(emptyList())
    val recentFiles by viewModel.recentFileHeader.collectAsState(emptyList())
    val singleFilePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
            result?.let {
                fileUri = it
                onOpenFile(fileUri.toString())
            }
        }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    singleFilePicker.launch("*/*")
                 },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Import file")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onBackIsPressed = onBackIsPressed
            )

            AudioFilesSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                fileHeaders = assetFiles,
                onClick = { header -> onOpenFile(header.fileUri) }
            )
        }
    }
}

@Composable
fun Header(modifier: Modifier, onBackIsPressed: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackIsPressed,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F5))
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Navigate Back",
                    tint = Color(0xFF303040),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .padding(top = 60.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF303040)
                )
            )

            Text(
                "Select your favorite music to play",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF606070),
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun AudioFilesSection(
    modifier: Modifier,
    fileHeaders: List<FileHeader>,
    onClick: (FileHeader) -> Unit
) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
              painter =   painterResource(R.drawable.icons8_music_folder_94),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Text(
                "Audio Library",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF303040)
                )
            )
        }

        if (fileHeaders.isNotEmpty()) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(fileHeaders) { header ->
                    AudioFileCard(
                        modifier = Modifier.fillMaxWidth(),
                        fileHeader = header,
                        onClick = onClick
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No audio files found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AudioFileCard(
    modifier: Modifier,
    fileHeader: FileHeader,
    onClick: (FileHeader) -> Unit
) {
    val colorPair = remember {
        modernColorPalette[Random.nextInt(modernColorPalette.size)]
    }

    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(colorPair.first, colorPair.second)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(fileHeader) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradientBrush)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = fileHeader.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF303040)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileDetailItem(
                        label = "Size",
                        value = fileHeader.mbSizeSt,
                        modifier = Modifier.weight(1f)
                    )

                    FileDetailItem(
                        label = "Duration",
                        value = fileHeader.durationSt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FileDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF606070).copy(alpha = 0.7f)
            )
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF303040),
                fontWeight = FontWeight.Medium
            )
        )
    }
}