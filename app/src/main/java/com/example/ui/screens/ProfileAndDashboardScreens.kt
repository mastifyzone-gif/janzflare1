package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CreatorProfile
import com.example.data.model.JanzCatalog
import com.example.data.model.Video
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.JanzFlareMark
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.VideoThumbnailCard
import com.example.ui.components.formatCount
import com.example.ui.components.formatDuration
import com.example.ui.components.formatJoinDate
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareAmber
import com.example.ui.theme.FlareOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    creatorId: String,
    isBottomTab: Boolean,
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenShorts: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOwnProfile = creatorId == viewModel.currentUserId

    val ownCreatorState by viewModel.ownCreatorState.collectAsStateWithLifecycle()
    val allCreators by viewModel.allCreators.collectAsStateWithLifecycle()
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val creator = if (isOwnProfile) {
        (ownCreatorState as? UiState.Success)?.data
    } else {
        allCreators.firstOrNull { it.creatorId == creatorId }
    }
    val user = (userState as? UiState.Success)?.data
    val creatorVideos = remember(isOwnProfile, creatorId, publicVideosState, myVideosState) {
        if (isOwnProfile) {
            (myVideosState as? UiState.Success)?.data.orEmpty().sortedByDescending { it.createdAt?.seconds ?: 0L }
        } else {
            (publicVideosState as? UiState.Success)?.data.orEmpty()
                .filter { it.creatorId == creatorId }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    val allPublic = (publicVideosState as? UiState.Success)?.data.orEmpty()
    val savedVideos = remember(user, allPublic) {
        val ids = user?.savedVideoIds?.toSet().orEmpty()
        allPublic.filter { it.id in ids }
    }

    val followRecord = following.firstOrNull { it.creatorId == creatorId }
    val isFollowing = followRecord != null
    val notifyOnUpload = followRecord?.notifyOnUpload ?: true
    val isBlocked = user?.blockedUserIds?.contains(creatorId) == true

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReportCreatorDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var reportVideoTarget by remember { mutableStateOf<Video?>(null) }
    var playlistVideoId by remember { mutableStateOf<String?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }

    val tabs = if (isOwnProfile) {
        listOf("Videos", "Shorts", "Playlists", "Saved", "About")
    } else {
        listOf("Videos", "Shorts", "About")
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = creator?.displayName ?: "Creator Profile",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (!isBottomTab) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            },
            actions = {
                if (isOwnProfile) {
                    IconButton(
                        onClick = onOpenDashboard,
                        modifier = Modifier.testTag("profile_dashboard_button")
                    ) {
                        Icon(Icons.Default.Analytics, "Creator Dashboard")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("profile_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock User" else "Block User") },
                                leadingIcon = { Icon(Icons.Default.Block, null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleBlockUser(creatorId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Creator") },
                                leadingIcon = { Icon(Icons.Default.Flag, null) },
                                onClick = {
                                    menuExpanded = false
                                    showReportCreatorDialog = true
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover Banner + Avatar + Header Info
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        if (!creator?.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = creator?.coverUrl,
                                contentDescription = "Cover photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_creator_banner),
                                contentDescription = "Channel cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))
                                )
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(12.dp))
                        if (creator?.founderAccount == true) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FlareOrange.copy(alpha = 0.14f),
                                border = BorderStroke(
                                    1.2.dp,
                                    Brush.linearGradient(listOf(FlareAmber, FlareOrange, ElectricViolet))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    JanzFlareMark(size = 22.dp, animated = false)
                                    Text(
                                        text = "OFFICIAL FOUNDER • JanzFlare का मालिक",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = FlareOrange
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CreatorAvatar(
                                avatarUrl = creator?.avatarUrl.orEmpty(),
                                name = creator?.displayName ?: "Creator",
                                size = 72.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = creator?.displayName ?: "Creator",
                                        style = MaterialTheme.typography.headlineMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (creator?.verified == true || creator?.affiliatedBrand?.isNotBlank() == true || creator?.founderAccount == true) {
                                        Spacer(Modifier.width(6.dp))
                                        VerifiedAffiliationBadges(
                                            verified = creator.verified,
                                            affiliatedBrand = creator.affiliatedBrand,
                                            founderAccount = creator.founderAccount,
                                            displayName = creator.displayName,
                                            joinedAt = creator.createdAt,
                                            iconSize = 20.dp
                                        )
                                    }
                                }
                                Text(
                                    text = "@${creator?.username ?: "creator"} • Joined ${formatJoinDate(creator?.createdAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!creator?.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(creator!!.bio, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(12.dp))

                        // Real Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill("Subscribers", formatCount(creator?.followersCount ?: 0L))
                            StatPill("Subscribing", formatCount(creator?.followingCount ?: 0L))
                            StatPill("Videos", formatCount(creator?.totalVideos ?: creatorVideos.size.toLong()))
                            StatPill("Views", formatCount(creator?.totalViews ?: 0L))
                        }

                        Spacer(Modifier.height(12.dp))

                        if (isOwnProfile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Profile")
                                }
                                Button(
                                    onClick = onOpenDashboard,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Analytics, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Studio")
                                }
                            }
                        } else if (creator != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFollowing) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFollow(creator) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Subscribed")
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (notifyOnUpload) FlareOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleSubscriptionBell(creator, notifyOnUpload) }
                                        ) {
                                            Icon(
                                                imageVector = if (notifyOnUpload) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = if (notifyOnUpload) "Notifications ON" else "Notifications muted",
                                                tint = if (notifyOnUpload) FlareOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.toggleFollow(creator) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Subscribe")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            val currentTabName = tabs.getOrElse(selectedTab) { "Videos" }
            when (currentTabName) {
                "Videos" -> {
                    val standardVideos = creatorVideos.filter { !it.shortForm }
                    if (standardVideos.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Standard Videos Yet",
                                subtitle = if (isOwnProfile) "Upload your first video to build your JanzFlare channel." else "This creator hasn't published standard videos yet.",
                                actionLabel = if (isOwnProfile) "Upload Video" else null,
                                onActionClick = if (isOwnProfile) onOpenUpload else null,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(standardVideos, key = { it.id }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                isLiked = likedIds.contains(video.id),
                                isSaved = user?.savedVideoIds?.contains(video.id) == true,
                                onPlayClick = { onOpenVideo(video.id) },
                                onCreatorClick = {},
                                onLikeClick = { viewModel.toggleLike(video) },
                                onCommentClick = { onOpenVideo(video.id) },
                                onShareClick = { shareVideoIntent(context, video) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                creatorVerified = creator?.verified ?: video.creatorVerified,
                                creatorAffiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                                creatorFounder = creator?.founderAccount == true,
                                creatorJoinedAt = creator?.createdAt
                            )
                        }
                    }
                }

                "Shorts" -> {
                    val shorts = creatorVideos.filter { it.shortForm || it.category == "Shorts" }
                    if (shorts.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Shorts Yet",
                                subtitle = "Vertical 9:16 Shorts will appear here.",
                                actionLabel = if (isOwnProfile) "Create Short" else null,
                                onActionClick = if (isOwnProfile) onOpenUpload else null,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(shorts, key = { it.id }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                isLiked = likedIds.contains(video.id),
                                isSaved = user?.savedVideoIds?.contains(video.id) == true,
                                onPlayClick = onOpenShorts,
                                onCreatorClick = {},
                                onLikeClick = { viewModel.toggleLike(video) },
                                onCommentClick = { onOpenVideo(video.id) },
                                onShareClick = { shareVideoIntent(context, video) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                creatorVerified = creator?.verified ?: video.creatorVerified,
                                creatorAffiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                                creatorFounder = creator?.founderAccount == true,
                                creatorJoinedAt = creator?.createdAt
                            )
                        }
                    }
                }

                "Playlists" -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Playlists", style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = { showNewPlaylistDialog = true }) {
                                Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Playlist")
                            }
                        }
                    }
                    if (playlists.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Playlists Created",
                                subtitle = "Organize favorite Indian creator videos into custom playlists.",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { pl ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable {
                                        pl.videoIds.firstOrNull()?.let(onOpenVideo)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(pl.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${pl.videoIds.size} videos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                "Saved" -> {
                    if (savedVideos.isEmpty()) {
                        item {
                            EmptyStudioCard(
                                title = "No Saved Videos",
                                subtitle = "Bookmark videos to access them quickly here.",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(savedVideos, key = { "saved_${it.id}" }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                isLiked = likedIds.contains(video.id),
                                isSaved = true,
                                onPlayClick = { onOpenVideo(video.id) },
                                onCreatorClick = {},
                                onLikeClick = { viewModel.toggleLike(video) },
                                onCommentClick = { onOpenVideo(video.id) },
                                onShareClick = { shareVideoIntent(context, video) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                "About" -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = if (creator?.founderAccount == true) {
                                BorderStroke(1.2.dp, Brush.linearGradient(listOf(FlareAmber, FlareOrange, ElectricViolet)))
                            } else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (creator?.founderAccount == true) {
                                    Surface(
                                        color = FlareOrange.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "JanzFlare का मालिक • Official Founder Account",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = FlareOrange,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                Text("About ${creator?.displayName ?: "Channel"}", style = MaterialTheme.typography.titleMedium)
                                Text(creator?.bio.orEmpty().ifBlank { "No bio added yet." }, style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Joined JanzFlare: ${formatJoinDate(creator?.createdAt)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!creator?.socialLink.isNullOrBlank()) {
                                    Text(
                                        "Social: ${creator!!.socialLink}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Total Watch Time: ${formatDuration(creator?.totalWatchTimeSec ?: 0L)}",
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

    if (showEditDialog && creator != null) {
        EditProfileDialog(
            creator = creator,
            preferredLanguage = user?.preferredLanguage ?: "Hindi",
            interests = user?.interests.orEmpty(),
            onDismiss = { showEditDialog = false },
            onSave = { name, username, bio, social, lang, newInterests, avatarUri, coverUri ->
                viewModel.updateProfile(
                    displayName = name,
                    username = username,
                    bio = bio,
                    socialLink = social,
                    preferredLanguage = lang,
                    interests = newInterests,
                    avatarUri = avatarUri,
                    coverUri = coverUri,
                    onDone = { showEditDialog = false }
                )
            }
        )
    }

    if (showReportCreatorDialog) {
        ReportContentDialog(
            targetTitle = "Creator",
            onDismiss = { showReportCreatorDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitReport("user", creatorId, "", reason, details)
                showReportCreatorDialog = false
            }
        )
    }

    reportVideoTarget?.let { target ->
        ReportContentDialog(
            targetTitle = "Video",
            onDismiss = { reportVideoTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport("video", target.id, target.id, reason, details)
                reportVideoTarget = null
            }
        )
    }

    playlistVideoId?.let { vid ->
        PlaylistPickerDialog(
            videoId = vid,
            playlists = playlists,
            onTogglePlaylist = { pl -> viewModel.toggleVideoInPlaylist(pl, vid) },
            onCreatePlaylist = { title, desc -> viewModel.createPlaylist(title, desc, vid) },
            onDismiss = { playlistVideoId = null }
        )
    }

    if (showNewPlaylistDialog) {
        var plTitle by remember { mutableStateOf("") }
        var plDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("Create Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = plTitle, onValueChange = { plTitle = it.take(80) }, label = { Text("Title") })
                    OutlinedTextField(value = plDesc, onValueChange = { plDesc = it.take(300) }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPlaylist(plTitle, plDesc)
                        showNewPlaylistDialog = false
                    },
                    enabled = plTitle.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    creator: CreatorProfile,
    preferredLanguage: String,
    interests: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<String>, Uri?, Uri?) -> Unit
) {
    var displayName by remember { mutableStateOf(creator.displayName) }
    var username by remember { mutableStateOf(creator.username) }
    var bio by remember { mutableStateOf(creator.bio) }
    var socialLink by remember { mutableStateOf(creator.socialLink) }
    var selectedLang by remember { mutableStateOf(preferredLanguage) }
    var selectedInterests by remember { mutableStateOf(interests.toSet()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) avatarUri = uri
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coverUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Creator Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (avatarUri != null) "Avatar ✓" else "Change Avatar") }
                    OutlinedButton(
                        onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (coverUri != null) "Cover ✓" else "Change Cover") }
                }
                OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(60) }, label = { Text("Display Name") })
                OutlinedTextField(value = username, onValueChange = { username = it.take(30) }, label = { Text("Username") })
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(300) }, label = { Text("Bio") }, maxLines = 3)
                OutlinedTextField(value = socialLink, onValueChange = { socialLink = it.take(200) }, label = { Text("Social Link") })
                Text("Preferred Language", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JanzCatalog.languages.take(8).forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) }
                        )
                    }
                }
                Text("Feed Interests", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JanzCatalog.allCategories.take(12).forEach { cat ->
                        val active = cat in selectedInterests
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedInterests = if (active) selectedInterests - cat else selectedInterests + cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(displayName, username, bio, socialLink, selectedLang, selectedInterests.toList(), avatarUri, coverUri)
                },
                enabled = displayName.isNotBlank() && username.trim().length >= 3
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorDashboardScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val ownCreatorState by viewModel.ownCreatorState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()

    val creator = (ownCreatorState as? UiState.Success)?.data
    val videos = (myVideosState as? UiState.Success)?.data.orEmpty()

    val totalViews = videos.sumOf { it.viewsCount }
    val totalLikes = videos.sumOf { it.likesCount }
    val totalComments = videos.sumOf { it.commentsCount }
    val totalWatchSec = videos.sumOf { it.watchTimeTotalSec }
    val engagementRate = if (totalViews > 0L) {
        String.format("%.1f%%", ((totalLikes + totalComments) * 100.0) / totalViews)
    } else "0.0%"

    val topVideos = remember(videos) { videos.sortedByDescending { it.viewsCount + it.likesCount * 2L } }
    var editingVideo by remember { mutableStateOf<Video?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Creator Studio", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Real Analytics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Channel Analytics (Live Firestore Data)", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Total Views", formatCount(totalViews), Modifier.weight(1f))
                        MetricCard("Subscribers", formatCount(creator?.followersCount ?: 0L), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Videos", videos.size.toString(), Modifier.weight(1f))
                        MetricCard("Watch Time", formatDuration(totalWatchSec), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Engagement Rate", engagementRate, Modifier.weight(1f))
                        MetricCard("Total Likes", formatCount(totalLikes), Modifier.weight(1f))
                    }
                }
            }

            // Monetization - Coming Soon (Zero fake earnings!)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = FlareOrange)
                                Spacer(Modifier.width(8.dp))
                                Text("Creator Monetization", style = MaterialTheme.typography.titleLarge)
                            }
                            Surface(
                                color = ElectricViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "COMING SOON",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricViolet,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            "Monetization activates once payment & eligibility verification launches. No estimated or placeholder earnings are shown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Upcoming Programs: Advertising Revenue • Creator Rewards • Brand Sponsorships • Premium Membership • Paid Content • Live Donations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Your Videos (${videos.size})", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onOpenUpload) { Text("Upload") }
                }
            }

            if (topVideos.isEmpty()) {
                item {
                    EmptyStudioCard(
                        title = "No Videos Uploaded Yet",
                        subtitle = "Upload your first video to start seeing real views, watch time, and engagement.",
                        actionLabel = "Upload Video",
                        onActionClick = onOpenUpload
                    )
                }
            } else {
                items(topVideos, key = { it.id }) { video ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenVideo(video.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    video.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${video.visibility.uppercase()} • ${video.category} • ${formatCount(video.viewsCount)} views • ${formatCount(video.likesCount)} likes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingVideo = video }) {
                                Icon(Icons.Default.Edit, "Edit video")
                            }
                            IconButton(onClick = { viewModel.deleteOwnVideo(video.id) }) {
                                Icon(Icons.Default.Delete, "Delete video", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    editingVideo?.let { target ->
        var editTitle by remember { mutableStateOf(target.title) }
        var editDesc by remember { mutableStateOf(target.description) }
        var editVis by remember { mutableStateOf(target.visibility) }
        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Edit Video Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it.take(120) }, label = { Text("Title") })
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it.take(2000) }, label = { Text("Description") }, maxLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("public", "unlisted", "private").forEach { vis ->
                            FilterChip(
                                selected = editVis == vis,
                                onClick = { editVis = vis },
                                label = { Text(vis.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOwnVideo(target.id, editTitle, editDesc, target.category, target.language, editVis)
                        editingVideo = null
                    },
                    enabled = editTitle.trim().length >= 2
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingVideo = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
