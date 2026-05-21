package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUri: String,
    title: String,
    isAudioOnly: Boolean = false,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Subtitle state variables
    var subtitleCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var subtitleDelayMs by remember { mutableStateOf(0L) }
    var currentSubtitleText by remember { mutableStateOf("") }
    var subtitleFileName by remember { mutableStateOf("") }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(videoUri) {
        val lastPosition = viewModel.getHistoryPosition(videoUri)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(videoUri))
            .build()
            
        exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (lastPosition > 0) {
            exoPlayer.seekTo(lastPosition)
        }
        exoPlayer.playWhenReady = true
    }
    
    // Save history periodically
    LaunchedEffect(exoPlayer) {
        while(true) {
            delay(5000)
            if (exoPlayer.isPlaying) {
                viewModel.saveHistory(
                    videoUri = videoUri,
                    title = title,
                    isAudioOnly = isAudioOnly,
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration.coerceAtLeast(0)
                )
            }
        }
    }

    // Dynamic custom subtitle checker loop
    LaunchedEffect(exoPlayer, subtitleCues, subtitleDelayMs) {
        while (true) {
            delay(80) 
            val targetPos = exoPlayer.currentPosition + subtitleDelayMs
            val activeCue = subtitleCues.firstOrNull { cue ->
                targetPos in cue.startTimeMs..cue.endTimeMs
            }
            currentSubtitleText = activeCue?.text ?: ""
        }
    }

    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val text = readTextFromUri(context, uri)
            subtitleCues = parseSubtitles(text)
            subtitleFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom.srt"
        }
    }

    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isFullScreen by remember { mutableStateOf(false) }
    val activity = context as? Activity

    // Reactive picture-in-picture state tracer loop
    var isInPipMode by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val appInPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
            if (isInPipMode != appInPip) {
                isInPipMode = appInPip
            }
            delay(250)
        }
    }

    // Auto-hiding control states after 3.5 seconds when active playing
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val isInPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
                    if (!isAudioOnly && !isInPip) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!isAudioOnly && activity?.isChangingConfigurations == false && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true)) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(isFullScreen) {
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
    
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Animation for pulsing circles on Audio visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "AcousticPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!isAudioOnly) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = showControls && !isLocked && !isInPipMode
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { playerView ->
                        playerView.useController = showControls && !isLocked && !isInPipMode
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Seamless overlay to trigger control appearance upon tap if they disappear
                if (!showControls && !isLocked && !isInPipMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showControls = true }
                    )
                }
            }
        } else {
            // High UX / Immersive visualizer dashboard for Audio Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06080E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎧 BoysPlayer Audio Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Neon pulsing disk visualizer
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(if (isPlaying) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF0F121C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Pulsing Beats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SAFE OUT-OF-FOCUS & SCREEN-OFF PLAYER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Custom control widgets on layout
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    FilledIconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Subtitles custom text overlays
        if (currentSubtitleText.isNotEmpty() && !isLocked && !isAudioOnly) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentSubtitleText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isLocked && !isInPipMode) {
            Box(modifier = Modifier.fillMaxSize().clickable { /* intercept touches */ })
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .padding(top = 32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock Dashboard", tint = Color.White)
            }
        } else if (!isLocked && showControls && !isInPipMode) {
            // Elegant glowing action header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                
                // Playback speed selection label
                TextButton(
                    onClick = {
                        playbackSpeed = when (playbackSpeed) {
                            1f -> 1.5f
                            1.5f -> 2f
                            2f -> 4f
                            4f -> 0.5f
                            else -> 1f
                        }
                        exoPlayer.setPlaybackParameters(PlaybackParameters(playbackSpeed))
                    },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Text("${playbackSpeed}x", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.width(4.dp))

                // PiP trigger
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity?.enterPictureInPictureMode(params)
                        } catch (e: Exception) {
                            Log.e("PlayerScreen", "Failed to enter PIP Mode", e)
                        }
                    }
                }) {
                    Icon(Icons.Default.PictureInPicture, contentDescription = "Picture-in-picture", tint = Color.White)
                }

                // Subtitle launcher modal
                IconButton(onClick = { showSubtitleDialog = true }) {
                    Icon(Icons.Default.Subtitles, contentDescription = "Subtitle Loader", tint = Color.White)
                }

                // Controls Lock
                IconButton(onClick = { isLocked = true }) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Lock controls", tint = Color.White)
                }

                // Fullscreen toggle orientation
                if (!isAudioOnly) {
                    IconButton(onClick = { isFullScreen = !isFullScreen }) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                            contentDescription = "Toggle orientation", 
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Interactive subtitle synchronization selector dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = {
                Text(
                    text = "Subtitles & Synchronization",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            subtitleLauncher.launch("*/*")
                            showSubtitleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick .SRT or .VTT File", color = Color.Black)
                    }

                    if (subtitleFileName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Successful", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Loaded: $subtitleFileName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text(
                        text = "Synchronize Subtitle Stream:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { subtitleDelayMs -= 500 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-0.5s", color = MaterialTheme.colorScheme.onSurface)
                        }

                        Text(
                            text = if (subtitleDelayMs >= 0) "+${subtitleDelayMs / 1000f}s" else "${subtitleDelayMs / 1000f}s",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { subtitleDelayMs += 500 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+0.5s", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Text(
                        text = "Adjust delay. Minus (-) triggers subtitles quicker; plus (+) delays them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Read plain text directly from chosen system file URI safely
fun readTextFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText() }
        } ?: ""
    } catch (e: Exception) {
        Log.e("PlayerScreen", "Error reading subtitle stream from Uri: $uri", e)
        ""
    }
}

// Complete inline robust SRT / VTT cue timeline parser
fun parseSubtitles(content: String): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    val lines = content.lines().map { it.trim() }
    
    val isVtt = lines.firstOrNull()?.contains("WEBVTT", ignoreCase = true) == true
    
    var idx = 0
    while (idx < lines.size) {
        val line = lines[idx]
        if (line.isEmpty() || line.all { it.isDigit() }) {
            idx++
            continue
        }
        
        if (line.contains("-->")) {
            val parts = line.split("-->").map { it.trim() }
            if (parts.size >= 2) {
                val startStr = parts[0]
                val endStr = parts[1].split(" ").first()
                
                val startMs = parseTimestampToMs(startStr, isVtt)
                val endMs = parseTimestampToMs(endStr, isVtt)
                
                val textBuilder = StringBuilder()
                idx++
                while (idx < lines.size && lines[idx].isNotEmpty() && !lines[idx].contains("-->")) {
                    if (textBuilder.isNotEmpty()) {
                        textBuilder.append("\n")
                    }
                    textBuilder.append(lines[idx])
                    idx++
                }
                
                if (startMs >= 0 && endMs >= 0) {
                    cues.add(SubtitleCue(startMs, endMs, textBuilder.toString()))
                }
                continue
            }
        }
        idx++
    }
    return cues
}

// Formatter translation logic to milliseconds
fun parseTimestampToMs(timeStr: String, isVtt: Boolean): Long {
    return try {
        val cleanStr = timeStr.replace(',', '.')
        val parts = cleanStr.split(":")
        if (parts.size == 3) {
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val secondsParts = parts[2].split(".")
            val seconds = secondsParts[0].toLong()
            val ms = if (secondsParts.size > 1) secondsParts[1].padEnd(3, '0').take(3).toLong() else 0L
            ((hours * 3600 + minutes * 60 + seconds) * 1000) + ms
        } else if (parts.size == 2) {
            val minutes = parts[0].toLong()
            val secondsParts = parts[1].split(".")
            val seconds = secondsParts[0].toLong()
            val ms = if (secondsParts.size > 1) secondsParts[1].padEnd(3, '0').take(3).toLong() else 0L
            ((minutes * 60 + seconds) * 1000) + ms
        } else {
            -1L
        }
    } catch (e: Exception) {
        -1L
    }
}
