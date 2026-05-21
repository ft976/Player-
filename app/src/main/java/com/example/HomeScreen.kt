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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
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
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161922)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF242938)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .videoFrameMillis(1000)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay clean play node
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Size: ${video.size / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    if (video.durationMillis > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatDuration(video.durationMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Ready to Play", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = info.title, 
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (info.videoUrl != null) {
                                Button(
                                    onClick = { onPlayYoutube(info.videoUrl, info.title, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play Live Video (upto 4x)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = { streamToAdd = info; isAddAudioOnly = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F354A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Video to Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (info.audioUrl != null) {
                                HorizontalDivider(color = Color(0xFF2F354A), modifier = Modifier.padding(vertical = 4.dp))
                                
                                Button(
                                    onClick = { onPlayYoutube(info.audioUrl, info.title, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play Audio Only (Screen-off safe)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = { streamToAdd = info; isAddAudioOnly = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F354A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Audio only to Playlist", color = Color.White, fontWeight = FontWeight.Bold)
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
