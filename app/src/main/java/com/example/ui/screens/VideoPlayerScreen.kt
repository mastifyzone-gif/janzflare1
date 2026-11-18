package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Comment
import com.example.data.model.Video
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.formatCount
import com.example.ui.components.formatDuration
import com.example.ui.components.formatRelativeDate
import com.example.ui.components.isLocalFileMissing
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.FlareOrange
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onSelectVideo: (String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()

    val allVideos = remember(publicVideosState, myVideosState) {
        val pub = (publicVideosState as? UiState.Success)?.data.orEmpty()
        val mine = (myVideosState as? UiState.Success)?.data.orEmpty()
        (pub + mine).distinctBy { it.id }
    }
    val video = allVideos.firstOrNull { it.id == videoId }
    val creator = creators.firstOrNull { it.creatorId == video?.creatorId }
    val user = (userState as? UiState.Success)?.data
    val isLiked = likedIds.contains(videoId)
    val isSaved = user?.savedVideoIds?.contains(videoId) == true
    val followRecord = following.firstOrNull { it.creatorId == video?.creatorId }
    val isFollowing = followRecord != null
    val notifyOnUpload = followRecord?.notifyOnUpload ?: true
    val upNextVideos = remember(allVideos, videoId) {
        allVideos.filter { it.id != videoId && !it.shortForm }.take(8)
    }

    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var volumeLevel by remember { mutableFloatStateOf(1.0f) }
    var muted by remember { mutableStateOf(false) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var theaterMode by remember { mutableStateOf(false) }
    var fullscreenMode by remember { mutableStateOf(false) }
    var qualityLabel by remember { mutableStateOf("Auto") }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var qualityMenuOpen by remember { mutableStateOf(false) }
    var playerErrorMessage by remember(videoId, video?.videoUri) { mutableStateOf<String?>(null) }
    val localVideoMissing = remember(video?.videoUri) {
        video != null && isLocalFileMissing(video.videoUri)
    }

    var commentText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showDescriptionExpanded by remember { mutableStateOf(false) }

    // Set active video for comment listener
    LaunchedEffect(videoId) {
        viewModel.setActiveWatchVideo(videoId)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setActiveWatchVideo(null) }
    }

    // Handle fullscreen orientation & system bars
    LaunchedEffect(fullscreenMode) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (fullscreenMode) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = fullscreenMode) {
        fullscreenMode = false
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(video?.videoUri, localVideoMissing) {
        val uri = video?.videoUri.orEmpty()
        playerErrorMessage = null
        if (uri.isNotBlank() && !localVideoMissing) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playerErrorMessage = error.localizedMessage
                    ?: "Could not load video stream. Check your internet connection or video URL."
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && autoPlayNext && upNextVideos.isNotEmpty()) {
                    onSelectVideo(upNextVideos.first().id)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll position and record watch-time
    LaunchedEffect(exoPlayer, video?.id) {
        var countedInitialView = false
        var accumulatedSec = 0L
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (exoPlayer.isPlaying && video != null) {
                accumulatedSec += 1L
                if (!countedInitialView) {
                    countedInitialView = true
                    viewModel.recordWatch(video, deltaWatchSec = 1L, countNewView = true)
                } else if (accumulatedSec % 10L == 0L) {
                    viewModel.recordWatch(video, deltaWatchSec = 10L, countNewView = false)
                }
            }
            delay(1000L)
        }
    }

    fun applyQuality(label: String, maxHeight: Int?) {
        qualityLabel = label
        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        if (maxHeight == null) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, maxHeight)
        }
        exoPlayer.trackSelectionParameters = builder.build()
    }

    if (video == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Video unavailable or removed", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Player Surface
        val playerModifier = when {
            fullscreenMode -> Modifier.fillMaxSize()
            theaterMode -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
            else -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        Box(
            modifier = playerModifier
                .background(Color.Black)
                .clickable { controlsVisible = !controlsVisible }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (theaterMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = if (theaterMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (localVideoMissing || playerErrorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.86f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Playback unavailable",
                            tint = FlareOrange,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (localVideoMissing) {
                                "Video file not found on this phone"
                            } else {
                                "Unable to play video"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (localVideoMissing) {
                                "Older local gallery uploads are erased if the app was uninstalled, and only play on the device that uploaded them."
                            } else {
                                playerErrorMessage.orEmpty()
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!localVideoMissing) {
                                OutlinedButton(onClick = {
                                    playerErrorMessage = null
                                    exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUri))
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                }) {
                                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Retry", color = Color.White)
                                }
                            } else if (video.creatorId == viewModel.currentUserId) {
                                Button(onClick = {
                                    viewModel.deleteOwnVideo(video.id)
                                    onBack()
                                }) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete Broken")
                                }
                            }
                            OutlinedButton(onClick = onBack) {
                                Text("Back", color = Color.White)
                            }
                        }
                    }
                }
            } else if (controlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    // Top Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (fullscreenMode) fullscreenMode = false else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Auto-Play toggle
                            Text("Auto", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = autoPlayNext,
                                onCheckedChange = { autoPlayNext = it },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Playback Speed
                            Box {
                                IconButton(onClick = { speedMenuOpen = true }) {
                                    Icon(Icons.Default.Speed, "Playback speed", tint = Color.White)
                                }
                                DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                                    listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x" + if (playbackSpeed == speed) " ✓" else "") },
                                            onClick = {
                                                playbackSpeed = speed
                                                exoPlayer.playbackParameters = PlaybackParameters(speed)
                                                speedMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quality selector
                            Box {
                                IconButton(onClick = { qualityMenuOpen = true }) {
                                    Icon(Icons.Default.Settings, "Video quality", tint = Color.White)
                                }
                                DropdownMenu(expanded = qualityMenuOpen, onDismissRequest = { qualityMenuOpen = false }) {
                                    listOf(
                                        "Auto" to null,
                                        "1080p HD" to 1080,
                                        "720p HD" to 720,
                                        "480p SD" to 480,
                                        "360p" to 360
                                    ).forEach { (label, height) ->
                                        DropdownMenuItem(
                                            text = { Text(label + if (qualityLabel == label) " ✓" else "") },
                                            onClick = {
                                                applyQuality(label, height)
                                                qualityMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Theater mode
                            IconButton(onClick = { theaterMode = !theaterMode }) {
                                Icon(
                                    Icons.Default.Theaters,
                                    "Theater mode",
                                    tint = if (theaterMode) FlareOrange else Color.White
                                )
                            }

                            // Picture-in-Picture
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                IconButton(onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                }) {
                                    Icon(Icons.Default.PictureInPictureAlt, "Picture in Picture", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                        }) {
                            Icon(Icons.Default.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier.testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                        }) {
                            Icon(Icons.Default.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }

                    // Bottom Scrubber + Volume + Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        val safeDuration = durationMs.coerceAtLeast(1L)
                        Slider(
                            value = (currentPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { fraction ->
                                exoPlayer.seekTo((fraction * safeDuration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = FlareOrange,
                                activeTrackColor = FlareOrange,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatDuration(currentPosMs / 1000L)} / ${formatDuration(durationMs / 1000L)}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        muted = !muted
                                        exoPlayer.volume = if (muted) 0f else volumeLevel
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute toggle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Slider(
                                    value = if (muted) 0f else volumeLevel,
                                    onValueChange = { v ->
                                        volumeLevel = v
                                        muted = v == 0f
                                        exoPlayer.volume = v
                                    },
                                    modifier = Modifier.width(72.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$qualityLabel • ${playbackSpeed}x",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                IconButton(onClick = { fullscreenMode = !fullscreenMode }) {
                                    Icon(
                                        imageVector = if (fullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle fullscreen",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!fullscreenMode) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title & Metadata
                item {
                    Column {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatCount(video.viewsCount)} views • ${formatRelativeDate(video.createdAt)} • ${video.category} • ${video.language}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (video.description.isNotBlank() || video.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDescriptionExpanded = !showDescriptionExpanded }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (video.description.isNotBlank()) {
                                        Text(
                                            text = video.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = if (showDescriptionExpanded) 20 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (video.tags.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            video.tags.forEach { tag ->
                                                Text(
                                                    text = "#$tag",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AssistChip(
                            onClick = { viewModel.toggleLike(video) },
                            leadingIcon = {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            label = { Text(formatCount(video.likesCount)) }
                        )
                        AssistChip(
                            onClick = { shareVideoIntent(context, video) },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            label = { Text("Share") }
                        )
                        AssistChip(
                            onClick = { viewModel.toggleSavedVideo(video) },
                            leadingIcon = {
                                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null)
                            },
                            label = { Text(if (isSaved) "Saved" else "Save") }
                        )
                        AssistChip(
                            onClick = { showPlaylistDialog = true },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                            label = { Text("Playlist") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { viewModel.toggleWatchLater(video.id) }) {
                            Icon(Icons.Default.WatchLater, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Watch Later")
                        }
                        TextButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Flag, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Report")
                        }
                    }
                }

                // Creator Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCreatorProfile(video.creatorId) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatorAvatar(
                                avatarUrl = creator?.avatarUrl ?: video.creatorAvatarUrl,
                                name = creator?.displayName ?: video.creatorName,
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = creator?.displayName ?: video.creatorName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (creator?.verified == true || video.creatorVerified || creator?.affiliatedBrand?.isNotBlank() == true || creator?.founderAccount == true) {
                                        Spacer(Modifier.width(4.dp))
                                        VerifiedAffiliationBadges(
                                            verified = creator?.verified ?: video.creatorVerified,
                                            affiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                                            founderAccount = creator?.founderAccount == true,
                                            displayName = creator?.displayName ?: video.creatorName,
                                            joinedAt = creator?.createdAt,
                                            iconSize = 16.dp
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatCount(creator?.followersCount ?: 0L)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (video.creatorId != viewModel.currentUserId && creator != null) {
                                if (isFollowing) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        OutlinedButton(onClick = { viewModel.toggleFollow(creator) }) {
                                            Text("Subscribed")
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleSubscriptionBell(creator, notifyOnUpload) }
                                        ) {
                                            Icon(
                                                imageVector = if (notifyOnUpload) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = if (notifyOnUpload) "Upload notifications ON" else "Upload notifications muted",
                                                tint = if (notifyOnUpload) FlareOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Button(onClick = { viewModel.toggleFollow(creator) }) {
                                        Text("Subscribe")
                                    }
                                }
                            }
                        }
                    }
                }

                // Comments Section (with Creator Pin / Delete Management)
                item {
                    Column {
                        Text(
                            text = "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        if (replyTarget != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Replying to ${replyTarget!!.authorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { replyTarget = null }) { Text("Cancel") }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it.take(500) },
                                placeholder = { Text("Add a comment...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.addComment(
                                            video = video,
                                            text = commentText,
                                            parentCommentId = replyTarget?.id.orEmpty()
                                        )
                                        commentText = ""
                                        replyTarget = null
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Post comment", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                val topLevelComments = comments.filter { it.parentCommentId.isBlank() }
                val repliesByParent = comments.filter { it.parentCommentId.isNotBlank() }.groupBy { it.parentCommentId }

                items(topLevelComments, key = { it.id }) { comment ->
                    CommentItemCard(
                        comment = comment,
                        replies = repliesByParent[comment.id].orEmpty(),
                        isVideoOwner = video.creatorId == viewModel.currentUserId,
                        currentUserId = viewModel.currentUserId,
                        onReply = { replyTarget = comment },
                        onPin = { viewModel.togglePinComment(video.id, comment) },
                        onDelete = { id -> viewModel.deleteComment(video, id) }
                    )
                }

                // Up Next Queue
                if (upNextVideos.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        Text("Up Next", style = MaterialTheme.typography.titleLarge)
                    }
                    items(upNextVideos, key = { "next_${it.id}" }) { next ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVideo(next.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = next.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${next.creatorName} • ${formatCount(next.viewsCount)} views",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportContentDialog(
            targetTitle = "Video",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitReport("video", video.id, video.id, reason, details)
                showReportDialog = false
            }
        )
    }

    if (showPlaylistDialog) {
        PlaylistPickerDialog(
            videoId = video.id,
            playlists = playlists,
            onTogglePlaylist = { pl -> viewModel.toggleVideoInPlaylist(pl, video.id) },
            onCreatePlaylist = { title, desc -> viewModel.createPlaylist(title, desc, video.id) },
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@Composable
private fun CommentItemCard(
    comment: Comment,
    replies: List<Comment>,
    isVideoOwner: Boolean,
    currentUserId: String,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(comment.authorAvatarUrl, comment.authorName, size = 30.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.authorName,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (comment.pinned) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Pinned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        formatRelativeDate(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onReply) {
                    Icon(Icons.Default.Reply, "Reply", modifier = Modifier.size(18.dp))
                }
                if (isVideoOwner) {
                    IconButton(onClick = onPin) {
                        Icon(
                            Icons.Default.PushPin,
                            "Pin comment",
                            tint = if (comment.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isVideoOwner || comment.authorId == currentUserId) {
                    IconButton(onClick = { onDelete(comment.id) }) {
                        Icon(Icons.Default.Delete, "Delete comment", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)

            if (replies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    replies.forEach { reply ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${reply.authorName} • ${formatRelativeDate(reply.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(reply.text, style = MaterialTheme.typography.bodySmall)
                            }
                            if (isVideoOwner || reply.authorId == currentUserId) {
                                IconButton(
                                    onClick = { onDelete(reply.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete reply", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
