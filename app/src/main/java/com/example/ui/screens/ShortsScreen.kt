package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.media.CloudinaryUploader
import com.example.data.model.CreatorProfile
import com.example.data.model.Video
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.ShimmerBox
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.formatCount
import com.example.ui.components.isLocalFileMissing
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.FlareOrange
import kotlinx.coroutines.delay

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: JanzViewModel,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val shortsFeed by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val focusedShortId by viewModel.focusedShortId.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

    val user = (userState as? UiState.Success)?.data
    val followMap = remember(following) { following.associateBy { it.creatorId } }

    var commentsSheetVideo by remember { mutableStateOf<Video?>(null) }
    var reportVideoTarget by remember { mutableStateOf<Video?>(null) }
    var commentDraft by remember { mutableStateOf("") }

    if (shortsFeed.isEmpty()) {
        if (publicVideosState is UiState.Loading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.size(38.dp), shape = CircleShape, darkSurface = true)
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(end = 72.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(modifier = Modifier.size(36.dp), shape = CircleShape, darkSurface = true)
                        Spacer(Modifier.width(10.dp))
                        ShimmerBox(modifier = Modifier.width(120.dp).height(16.dp), darkSurface = true)
                    }
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp), darkSurface = true)
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp), darkSurface = true)
                }
            }
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isOnline) {
                    EmptyStudioCard(
                        title = "Weak or No Internet Connection",
                        subtitle = "Could not load Shorts right now. Check your network and tap Retry.",
                        actionLabel = "Retry",
                        onActionClick = viewModel::refreshFeed
                    )
                } else {
                    EmptyStudioCard(
                        title = "No Shorts Published Yet",
                        subtitle = "Swipe-ready 9:16 vertical videos appear here as soon as you or other creators upload Shorts.",
                        actionLabel = "Create a Short",
                        onActionClick = onOpenUpload
                    )
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shortsFeed.size })

    LaunchedEffect(focusedShortId, shortsFeed) {
        val targetId = focusedShortId
        if (!targetId.isNullOrBlank() && shortsFeed.isNotEmpty()) {
            val idx = shortsFeed.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                pagerState.scrollToPage(idx)
            }
            viewModel.clearFocusedShort()
        }
    }

    VerticalPager(
        state = pagerState,
        key = { page -> shortsFeed[page].id },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val shortVideo = shortsFeed[page]
        val isActive = pagerState.currentPage == page
        val creator = creators.firstOrNull { it.creatorId == shortVideo.creatorId }
        val isLiked = likedIds.contains(shortVideo.id)
        val isSaved = user?.savedVideoIds?.contains(shortVideo.id) == true
        val followRecord = followMap[shortVideo.creatorId]
        val isFollowing = followRecord != null
        val notifyOnUpload = followRecord?.notifyOnUpload ?: true

        val resolvedCreator = creator ?: CreatorProfile(
            creatorId = shortVideo.creatorId,
            displayName = shortVideo.creatorName.ifBlank { "Creator" },
            username = shortVideo.creatorUsername.ifBlank { "creator" },
            avatarUrl = shortVideo.creatorAvatarUrl,
            verified = shortVideo.creatorVerified
        )

        ShortPlayerPage(
            video = shortVideo,
            creator = creator,
            isActive = isActive,
            isOnline = isOnline,
            isLiked = isLiked,
            isSaved = isSaved,
            isFollowing = isFollowing,
            notifyOnUpload = notifyOnUpload,
            isSelf = shortVideo.creatorId == viewModel.currentUserId,
            onPublicViewCounted = { viewModel.recordWatch(shortVideo, 1L, countNewView = true, countEngagedView = false) },
            onEngagedViewCounted = { viewModel.recordWatch(shortVideo, 3L, countNewView = false, countEngagedView = true) },
            onLikeClick = { viewModel.toggleLike(shortVideo) },
            onCommentClick = {
                viewModel.setActiveWatchVideo(shortVideo.id)
                commentsSheetVideo = shortVideo
            },
            onShareClick = { shareVideoIntent(context, shortVideo, platformSettings) },
            onSaveClick = { viewModel.toggleSavedVideo(shortVideo) },
            onFollowClick = { viewModel.toggleFollow(resolvedCreator) },
            onToggleBell = { viewModel.toggleSubscriptionBell(resolvedCreator, notifyOnUpload) },
            onCreatorClick = { onOpenCreatorProfile(shortVideo.creatorId) },
            onReportClick = { reportVideoTarget = shortVideo }
        )
    }

    commentsSheetVideo?.let { targetVideo ->
        ModalBottomSheet(onDismissRequest = {
            commentsSheetVideo = null
            viewModel.setActiveWatchVideo(null)
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            CreatorAvatar(comment.authorAvatarUrl, comment.authorName, size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it.take(500) },
                        placeholder = { Text("Write a comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (commentDraft.isNotBlank()) {
                            viewModel.addComment(targetVideo, commentDraft)
                            commentDraft = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    }

    reportVideoTarget?.let { target ->
        ReportContentDialog(
            targetTitle = "Short",
            onDismiss = { reportVideoTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport("video", target.id, target.id, reason, details)
                reportVideoTarget = null
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortPlayerPage(
    video: Video,
    creator: CreatorProfile?,
    isActive: Boolean,
    isOnline: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    notifyOnUpload: Boolean,
    isSelf: Boolean,
    onPublicViewCounted: () -> Unit,
    onEngagedViewCounted: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFollowClick: () -> Unit,
    onToggleBell: () -> Unit,
    onCreatorClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val context = LocalContext.current
    var pausedByUser by remember(video.id) { mutableStateOf(false) }
    val localVideoMissing = remember(video.videoUri) { isLocalFileMissing(video.videoUri) }
    var activePlaybackUri by remember { mutableStateOf(video.videoUri) }
    var triedTranscodeFallback by remember { mutableStateOf(false) }
    var isPreparingFallback by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }

    val currentIsActive by rememberUpdatedState(isActive)
    val currentPausedByUser by rememberUpdatedState(pausedByUser)

    val exoPlayer = remember(video.id) {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(video.id, video.videoUri, localVideoMissing) {
        activePlaybackUri = video.videoUri
        triedTranscodeFallback = false
        isPreparingFallback = false
        playerErrorMessage = null
        if (video.videoUri.isNotBlank() && !localVideoMissing) {
            exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUri))
            exoPlayer.prepare()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    var countedPublic by remember(video.id) { mutableStateOf(false) }
    var countedEngaged by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(isActive, pausedByUser, localVideoMissing) {
        if (isActive && !pausedByUser && !localVideoMissing) {
            exoPlayer.playWhenReady = true
            if (!countedPublic) {
                countedPublic = true
                onPublicViewCounted()
            }
            if (!countedEngaged) {
                delay(3_000L)
                if (currentIsActive && !currentPausedByUser && !countedEngaged) {
                    countedEngaged = true
                    onEngagedViewCounted()
                }
            }
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    isPreparingFallback = false
                    playerErrorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val fallbackUrl = if (!triedTranscodeFallback) {
                    CloudinaryUploader.buildCompatibleVideoUrl(activePlaybackUri)
                } else {
                    null
                }
                if (fallbackUrl != null) {
                    triedTranscodeFallback = true
                    isPreparingFallback = true
                    playerErrorMessage = null
                    activePlaybackUri = fallbackUrl
                    exoPlayer.setMediaItem(MediaItem.fromUri(fallbackUrl))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = currentIsActive && !currentPausedByUser
                } else {
                    isPreparingFallback = false
                    playerErrorMessage = friendlyPlaybackErrorMessage(error)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { pausedByUser = !pausedByUser }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPreparingFallback || (isBuffering && !localVideoMissing && playerErrorMessage == null)) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = FlareOrange, modifier = Modifier.size(34.dp))
                Text(
                    text = when {
                        isPreparingFallback -> "Optimizing video for your phone..."
                        !isOnline -> "Weak or no internet connection — waiting to load..."
                        else -> "Loading Short..."
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (!isOnline) {
                    OutlinedButton(onClick = {
                        playerErrorMessage = null
                        val retryUri = activePlaybackUri.ifBlank { video.videoUri }
                        exoPlayer.setMediaItem(MediaItem.fromUri(retryUri))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else if (localVideoMissing || playerErrorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Warning, "Unavailable", tint = FlareOrange, modifier = Modifier.size(36.dp))
                Text(
                    text = if (localVideoMissing) {
                        "Short unavailable on this phone"
                    } else if (!isOnline) {
                        "Weak or no internet connection. Check network and tap Retry."
                    } else {
                        playerErrorMessage.orEmpty()
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                if (!localVideoMissing) {
                    OutlinedButton(onClick = {
                        playerErrorMessage = null
                        val retryUri = activePlaybackUri.ifBlank { video.videoUri }
                        exoPlayer.setMediaItem(MediaItem.fromUri(retryUri))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else if (pausedByUser) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }

        // Bottom Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
                )
        )

        // Right Vertical Actions Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) FlareOrange else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(formatCount(video.likesCount), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCommentClick) {
                    Icon(Icons.AutoMirrored.Filled.Comment, "Comments", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Text(formatCount(video.commentsCount), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onSaveClick) {
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    "Save",
                    tint = if (isSaved) FlareOrange else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onReportClick) {
                Icon(Icons.Default.Flag, "Report", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        // Bottom Creator Info & Caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 72.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(
                    avatarUrl = creator?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.creatorAvatarUrl,
                    name = video.creatorName,
                    size = 34.dp,
                    modifier = Modifier.clickable(onClick = onCreatorClick)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "@${video.creatorUsername}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(onClick = onCreatorClick)
                )
                if (creator?.verified == true || video.creatorVerified || creator?.affiliatedBrand?.isNotBlank() == true || creator?.founderAccount == true) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedAffiliationBadges(
                        verified = creator?.verified ?: video.creatorVerified,
                        affiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                        founderAccount = creator?.founderAccount == true,
                        displayName = creator?.displayName ?: video.creatorName,
                        joinedAt = creator?.createdAt,
                        iconSize = 15.dp
                    )
                }
                if (!isSelf) {
                    Spacer(Modifier.width(8.dp))
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollowClick,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Subscribed", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = onToggleBell, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (notifyOnUpload) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = if (notifyOnUpload) "Upload notifications ON" else "Upload notifications muted",
                                tint = if (notifyOnUpload) FlareOrange else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onFollowClick,
                            colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Subscribe", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = video.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (video.description.isNotBlank()) {
                Text(
                    text = video.description,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${video.category} • ${video.language} • Original Audio",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun friendlyPlaybackErrorMessage(error: PlaybackException): String {
    val msg = error.message.orEmpty()
    return when {
        msg.contains("EXCEEDS_CAPABILITIES", ignoreCase = true) ||
            msg.contains("MediaCodec", ignoreCase = true) ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "This video's format isn't supported on this phone."
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Network error while loading video. Check your connection and tap Retry."
        else -> "Could not play this video right now. Tap Retry."
    }
}
