package com.example

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlayLocalVideo: (LocalVideo) -> Unit,
    onPlayYoutube: (url: String, title: String, isAudioOnly: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val streamInfoResult by viewModel.youtubeStreamInfo.collectAsState()
    LaunchedEffect(streamInfoResult) {
        if (streamInfoResult != null && streamInfoResult!!.isSuccess) {
            selectedTab = 1
        }
    }
    
    // Modern cyberpunk themed container
    val appBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF080A0F),
            Color(0xFF0F121C)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(com.example.R.drawable.img_app_icon_1779376445176)
                                .crossfade(true)
                                .build(),
                            contentDescription = "BoysPlayer Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                                    append("Boys")
                                }
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)) {
                                    append("Player")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF080A0F),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Developer & Privacy Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF080A0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(appBgBrush)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF080A0F),
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFF1B1E28))
                }
            ) {
                listOf(
                    Pair("Local Files", Icons.Default.Folder),
                    Pair("Online / URL", Icons.Default.Link),
                    Pair("Playlists", Icons.Default.PlaylistPlay),
                    Pair("History", Icons.Default.History)
                ).forEachIndexed { i, tabInfo ->
                    Tab(
                        selected = selectedTab == i, 
                        onClick = { selectedTab = i }, 
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tabInfo.second,
                                    contentDescription = null,
                                    tint = if (selectedTab == i) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tabInfo.first, 
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTab == i) Color.White else Color.Gray
                                )
                            }
                        }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (selectedTab) {
                    0 -> LocalVideosTab(viewModel, onPlayLocalVideo)
                    1 -> YouTubeTab(viewModel, onPlayYoutube)
                    2 -> PlaylistsTab(viewModel, onPlayYoutube)
                    3 -> HistoryTab(viewModel, onPlayYoutube)
                }
            }
        }
    }

    if (showInfoDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "About BoysPlayer",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Developer Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161922))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "DEVELOPER PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rehan97",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.linkedin.com/in/rehan-ahmad-863386382?utm_source=share_via&utm_content=profile&utm_medium=member_android")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle gracefully
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Connect on LinkedIn",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Support & Platform details
                    Column {
                        Text(
                            text = "SUPPORT DOCUMENTATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Local video speed controls up to 4.0x, PiP mode integration & safe controller lock overlay.\n• Paste YouTube links to instantly extract media links, with specialized background playback (audio-only) mode that works safely with screen-off capabilities.\n• Custom playlist foldering structure to save external resource feeds locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    // Privacy Section
                    Column {
                        Text(
                            text = "PRIVACY STATEMENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BoysPlayer implements secure strict client-side streaming logic. Local files and directory contents are kept strictly on-device. No telemetry, log caches, or server-side media tracking is active. Dynamic network calls are solely addressed to secure Piped endpoints for on-the-fly streaming resolution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false }
                ) {
                    Text(
                        text = "CLOSE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color(0xFF0F121C),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun LocalVideosTab(viewModel: MainViewModel, onPlayLocalVideo: (LocalVideo) -> Unit) {
    val videos by viewModel.localVideos.collectAsState()
    val isLoading by viewModel.isLoadingVideos.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var videoToAdd by remember { mutableStateOf<LocalVideo?>(null) }

    // Direct platform file selection launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = try {
                val path = uri.path ?: ""
                val file = File(path)
                if (file.name.isNotBlank()) file.name else "Chosen Movie Stream"
            } catch (e: Exception) {
                "Selected File Video"
            }
            val video = LocalVideo(
                id = System.currentTimeMillis(),
                uri = uri,
                name = displayName,
                durationMillis = 0,
                size = 0,
                dateAdded = System.currentTimeMillis()
            )
            onPlayLocalVideo(video)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchLocalVideos()
    }

    if (videoToAdd != null) {
        AlertDialog(
            onDismissRequest = { videoToAdd = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one under the 'Playlists' tab first.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.addVideoToPlaylist(
                                            playlist.id,
                                            videoToAdd!!.uri.toString(),
                                            videoToAdd!!.name,
                                            false,
                                            videoToAdd!!.durationMillis,
                                            videoToAdd!!.size
                                        )
                                        videoToAdd = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { videoToAdd = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Aesthetic Quick Link selector action card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "File Hub explorer", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Browse custom folders, deep downloads, or external SD logs on your device.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { filePickerLauncher.launch("video/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No gallery videos auto-scanned",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Use the 'File Hub explorer' above to locate and play any files directly!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .weight(1f)
            ) {
                item {
                    Text(
                        text = "Scanned Gallery Videos (${videos.size})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(videos) { video ->
                    VideoItem(
                        video, 
                        onClick = { onPlayLocalVideo(video) },
                        onAddToPlaylist = { videoToAdd = video }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoItem(video: LocalVideo, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161922)
        )
    ) {
        Column {
            // Widescreen 16:9 Aspect Ratio video thumbnail with duration overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF10121A)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .videoFrameMillis(2000) // Frame at 2s is typically better than 1s
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail for ${video.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Translucent dim gradient at the bottom base
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // High UX stylized play button at the center
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Duration Overlay badge at the bottom right
                if (video.durationMillis > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMillis),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Text metadata row layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${video.size / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                var expanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.LightGray)
                    }
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E2230))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = { expanded = false; onAddToPlaylist() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeTab(viewModel: MainViewModel, onPlayYoutube: (url: String, title: String, isAudioOnly: Boolean) -> Unit) {
    var url by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoadingYoutube.collectAsState()
    val streamInfoResult by viewModel.youtubeStreamInfo.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState(initial = emptyList())

    var streamToAdd by remember { mutableStateOf<YouTubeStreamInfo?>(null) }
    var isAddAudioOnly by remember { mutableStateOf(false) }

    if (streamToAdd != null) {
        AlertDialog(
            onDismissRequest = { streamToAdd = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one under the 'Playlists' tab first.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val cUrl = if (isAddAudioOnly) streamToAdd!!.audioUrl else streamToAdd!!.videoUrl
                                        if (cUrl != null) {
                                            viewModel.addVideoToPlaylist(
                                                playlistId = playlist.id,
                                                uri = cUrl,
                                                title = streamToAdd!!.title,
                                                isAudioOnly = isAddAudioOnly
                                            )
                                        }
                                        streamToAdd = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { streamToAdd = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Neon Cyberpunk extraction interface
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161922)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CYBER STREAMER", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "HIGH RES", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), 
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Watch & Listen to YouTube", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), 
                        color = Color.White
                    )
                    Text(
                        "Paste link to experience 4x speeds, synchronization tools, and screen-off audio playback.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("Paste YouTube link here...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedContainerColor = Color(0xFF202534),
                            unfocusedContainerColor = Color(0xFF0F121C),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C3246)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.fetchYoutubeStream(url) },
                            enabled = url.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color(0xFF2C3246),
                                contentColor = Color.Black,
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = if (url.isNotBlank()) Color.Black else Color.Gray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXTRACT STREAMS", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        
        item {
            if (streamInfoResult?.isFailure == true) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF291015)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Failed to load link stream. Trying to switch fallback engines. Please verify link or retry.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            streamInfoResult?.getOrNull()?.let { info ->
                val context = LocalContext.current
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
                ) {
                    if (info.isPlaylist) {
                        // ==========================================
                        // PLAYLIST DETAIL RENDERING MODE
                        // ==========================================
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = "Playlist",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = info.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Playlist • by ${info.uploader ?: "Various Artists"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFF2F354A))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Videos in Playlist (${info.playlistVideos.size}):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Quick download all / Bulk action
                            if (info.playlistVideos.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Extracting and scheduling downloads for all playlist items...", Toast.LENGTH_LONG).show()
                                        // Loop and extract in the background or trigger sequentials
                                        info.playlistVideos.forEach { item ->
                                            viewModel.viewModelScope.launch {
                                                try {
                                                    val res = YouTubeRepository().getStreamInfo(item.videoId)
                                                    res.getOrNull()?.let { streamDetails ->
                                                        // Auto download best video/audio format
                                                        val finalUrl = streamDetails.audioUrl ?: streamDetails.videoUrl
                                                        if (finalUrl != null) {
                                                            val finalTitle = streamDetails.title
                                                            startDownload(context, finalUrl, finalTitle, true, "128kbps")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    // fail-silent
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Entire Playlist (All as MP3 Audio)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic items mapping list
                            info.playlistVideos.forEachIndexed { index, playlistVideo ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            url = "https://www.youtube.com/watch?v=${playlistVideo.videoId}"
                                            viewModel.fetchYoutubeStream(url)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(playlistVideo.thumbnailUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlistVideo.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = playlistVideo.uploader ?: "YouTube Artist",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                // Trigger fast stream extraction of this specific video so they can download it
                                                url = "https://www.youtube.com/watch?v=${playlistVideo.videoId}"
                                                viewModel.fetchYoutubeStream(url)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = "Extract Video",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // SINGLE VIDEO DETAIL RENDERING MODE
                        // ==========================================
                        Column {
                            // 16:9 Widescreen YouTube Video Thumbnail Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(info.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "YouTube Video Preview Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Dim gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                            )
                                        )
                                )
                                
                                // Glowing central play indicator
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.85f))
                                        .clickable {
                                            if (info.videoUrl != null) {
                                                onPlayYoutube(info.videoUrl, info.title, false)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Uploader / Channel tag
                                if (!info.uploader.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = info.uploader,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                // Title
                                Text(
                                    text = info.title, 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // Description snippet if available
                                if (!info.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = info.description.trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // PLAYBACK ACTIONS MODULE
                                Text(
                                    text = "FAST PLAYBACK SELECTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (info.videoUrl != null) {
                                        Button(
                                            onClick = { onPlayYoutube(info.videoUrl, info.title, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Play Video", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (info.audioUrl != null) {
                                        Button(
                                            onClick = { onPlayYoutube(info.audioUrl, info.title, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Play Audio Mode", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { streamToAdd = info; isAddAudioOnly = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F354A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add to Video Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color(0xFF2F354A))
                                Spacer(modifier = Modifier.height(16.dp))

                                // DOWNLOAD VIDEO AT SEVERAL QUALITIES (MP4)
                                Text(
                                    text = "⚡ DOWNLOAD VIDEO QUALITIES (MP4)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                val filteredFormats = info.videoFormats.filter { !it.videoOnly || it.format == "MPEG_4" || it.quality.isNotBlank() }
                                if (filteredFormats.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filteredFormats.forEach { format ->
                                            val label = if (format.videoOnly) "${format.quality} (Silent MP4 - No Audio)" else "${format.quality} (Full HD MP4)"
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Format: MP4 • Format codec: ${format.format}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            startDownload(context, format.url, info.title, false, format.quality)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = "Download Video",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Fallback when video formats list is empty
                                    Button(
                                        onClick = {
                                            if (info.videoUrl != null) {
                                                startDownload(context, info.videoUrl, info.title, false, "Standard Quality")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131722)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download Video (Standard Quality - MP4)", color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // DOWNLOAD AUDIO QUALITY OPTIONS (MP3)
                                Text(
                                    text = "🎵 DOWNLOAD AUDIO SOUNDTRACKS (MP3)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                if (info.audioFormats.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        info.audioFormats.forEach { audioFormat ->
                                            val bitrateKbps = if (audioFormat.bitrate > 0) "${audioFormat.bitrate / 1000}kbps" else "128kbps"
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Download MP3 ($bitrateKbps Quality)",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Format: MP3 • Bitrate: $bitrateKbps",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            startDownload(context, audioFormat.url, info.title, true, bitrateKbps)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = "Download Audio",
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Fallback when audio formats list is empty
                                    Button(
                                        onClick = {
                                            if (info.audioUrl != null) {
                                                startDownload(context, info.audioUrl, info.title, true, "Standard MP3")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131722)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download Audio Track (MP3)", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun startDownload(context: android.content.Context, url: String, title: String, isAudio: Boolean, qualityStr: String) {
    try {
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val cleanTitle = title.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        val ext = if (isAudio) "mp3" else "mp4"
        val label = if (isAudio) "MP3" else "MP4 ($qualityStr)"
        val filename = "$cleanTitle.$ext"

        val request = android.app.DownloadManager.Request(Uri.parse(url))
            .setTitle(cleanTitle)
            .setDescription("Downloading YouTube $label stream")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadManager.enqueue(request)
        Toast.makeText(context, "Download started: $cleanTitle ($label)", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
