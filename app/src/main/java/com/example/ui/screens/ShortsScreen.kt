package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.CreatorProfile
import com.example.data.model.Video
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.formatCount
import com.example.ui.components.isLocalFileMissing
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.FlareOrange

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: JanzViewModel,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shortsFeed by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()

    val user = (userState as? UiState.Success)?.data
    val followMap = remember(following) { following.associateBy { it.creatorId } }

    var commentsSheetVideo by remember { mutableStateOf<Video?>(null) }
    var reportVideoTarget by remember { mutableStateOf<Video?>(null) }
    var commentDraft by remember { mutableStateOf("") }

    if (shortsFeed.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStudioCard(
                title = "No Shorts Published Yet",
                subtitle = "Swipe-ready 9:16 vertical videos appear here as soon as you or other creators upload Shorts.",
                actionLabel = "Create a Short",
                onActionClick = onOpenUpload
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shortsFeed.size })

    VerticalPager(
        state = pagerState,
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

        ShortPlayerPage(
            video = shortVideo,
            creator = creator,
            isActive = isActive,
            isLiked = isLiked,
            isSaved = isSaved,
            isFollowing = isFollowing,
            notifyOnUpload = notifyOnUpload,
            isSelf = shortVideo.creatorId == viewModel.currentUserId,
            onViewCounted = { viewModel.recordWatch(shortVideo, 2L, countNewView = true) },
            onLikeClick = { viewModel.toggleLike(shortVideo) },
            onCommentClick = {
                viewModel.setActiveWatchVideo(shortVideo.id)
                commentsSheetVideo = shortVideo
            },
            onShareClick = { shareVideoIntent(context, shortVideo) },
            onSaveClick = { viewModel.toggleSavedVideo(shortVideo) },
            onFollowClick = { if (creator != null) viewModel.toggleFollow(creator) },
            onToggleBell = { if (creator != null) viewModel.toggleSubscriptionBell(creator, notifyOnUpload) },
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
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    notifyOnUpload: Boolean,
    isSelf: Boolean,
    onViewCounted: () -> Unit,
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
    var playerErrorMessage by remember(video.id, video.videoUri) { mutableStateOf<String?>(null) }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            if (video.videoUri.isNotBlank() && !isLocalFileMissing(video.videoUri)) {
                setMediaItem(MediaItem.fromUri(video.videoUri))
                prepare()
            }
        }
    }

    LaunchedEffect(isActive, pausedByUser, localVideoMissing) {
        if (isActive && !pausedByUser && !localVideoMissing) {
            exoPlayer.playWhenReady = true
            onViewCounted()
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerErrorMessage = error.localizedMessage ?: "Failed to play Short."
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

        if (localVideoMissing || playerErrorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, "Unavailable", tint = FlareOrange, modifier = Modifier.size(36.dp))
                Text(
                    text = if (localVideoMissing) {
                        "Short file unavailable on this phone"
                    } else {
                        playerErrorMessage.orEmpty()
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
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
                .padding(start = 16.dp, end = 78.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(
                    avatarUrl = video.creatorAvatarUrl,
                    name = video.creatorName,
                    size = 38.dp,
                    modifier = Modifier.clickable(onClick = onCreatorClick)
                )
                Spacer(Modifier.width(10.dp))
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
                    Spacer(Modifier.width(10.dp))
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollowClick,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Subscribed", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = onToggleBell, modifier = Modifier.size(34.dp)) {
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
                            shape = RoundedCornerShape(20.dp)
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
