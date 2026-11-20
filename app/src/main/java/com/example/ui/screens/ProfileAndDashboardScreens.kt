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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.MonetizationRequest
import com.example.data.model.Video
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.ImageCropRotateDialog
import com.example.ui.components.JanzFlareMark
import com.example.ui.components.NetworkStatusBanner
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.ProfileHeaderSkeleton
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.VideoCardSkeleton
import com.example.ui.components.VideoThumbnailCard
import com.example.ui.components.formatCount
import com.example.ui.components.formatDuration
import com.example.ui.components.formatJoinDate
import com.example.ui.components.isLocalFileMissing
import com.example.ui.components.isMediaUriAvailable
import com.example.ui.components.openExternalHttpsUrl
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareAmber
import com.example.ui.theme.FlareOrange
import java.util.Locale

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

    LaunchedEffect(creatorId, isOwnProfile) {
        viewModel.setTargetCreatorId(if (isOwnProfile) null else creatorId)
    }

    val ownCreatorState by viewModel.ownCreatorState.collectAsStateWithLifecycle()
    val targetCreatorState by viewModel.targetCreatorState.collectAsStateWithLifecycle()
    val allCreators by viewModel.allCreators.collectAsStateWithLifecycle()
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

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

    val targetCreator = (targetCreatorState as? UiState.Success)?.data
    val fallbackFromVideos = remember(creatorVideos, creatorId) {
        creatorVideos.firstOrNull()?.let { v ->
            CreatorProfile(
                creatorId = creatorId,
                displayName = v.creatorName.ifBlank { "Creator" },
                username = v.creatorUsername.ifBlank { "creator" },
                avatarUrl = v.creatorAvatarUrl,
                verified = v.creatorVerified,
                totalVideos = creatorVideos.size.toLong(),
                totalViews = creatorVideos.sumOf { it.viewsCount }
            )
        }
    }

    val creator = if (isOwnProfile) {
        (ownCreatorState as? UiState.Success)?.data
    } else {
        targetCreator
            ?: allCreators.firstOrNull { it.creatorId == creatorId }
            ?: fallbackFromVideos
    }

    val isLoadingCreator = creator == null && (
        (isOwnProfile && ownCreatorState is UiState.Loading) ||
            (!isOwnProfile && (targetCreatorState is UiState.Loading || publicVideosState is UiState.Loading))
        )
    val isLoadingVideos = if (isOwnProfile) myVideosState is UiState.Loading else publicVideosState is UiState.Loading

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

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshFeed,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isOnline) {
                    item {
                        NetworkStatusBanner(
                            isOnline = isOnline,
                            onRetry = viewModel::refreshFeed,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Cover Banner + Avatar + Header Info
                item {
                    if (isRefreshing || isLoadingCreator) {
                        ProfileHeaderSkeleton()
                    } else if (creator == null) {
                        EmptyStudioCard(
                            title = "Channel Unavailable",
                            subtitle = "Could not load this creator's profile. Please check your connection and try again.",
                            actionLabel = "Retry",
                            onActionClick = viewModel::refreshFeed,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val coverUrl = creator?.coverUrl.orEmpty()
                    val coverAvailable = remember(coverUrl) { isMediaUriAvailable(coverUrl) }
                    var coverError by remember(coverUrl) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        if (coverAvailable && !coverError) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Cover photo",
                                contentScale = ContentScale.Crop,
                                onError = { coverError = true },
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
                                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_studio_button")
                                ) {
                                    Icon(Icons.Default.Analytics, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Studio")
                                }
                            }
                        } else {
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
                    if (isRefreshing || (isLoadingVideos && standardVideos.isEmpty())) {
                        items(2) {
                            VideoCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else if (standardVideos.isEmpty()) {
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
                                onShareClick = { shareVideoIntent(context, video, platformSettings) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                creatorVerified = creator?.verified ?: video.creatorVerified,
                                creatorAffiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                                creatorFounder = creator?.founderAccount == true,
                                creatorJoinedAt = creator?.createdAt,
                                creatorAvatarUrl = creator?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.creatorAvatarUrl
                            )
                        }
                    }
                }

                "Shorts" -> {
                    val shorts = creatorVideos.filter { it.shortForm || it.category == "Shorts" }
                    if (isRefreshing || (isLoadingVideos && shorts.isEmpty())) {
                        items(2) {
                            VideoCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else if (shorts.isEmpty()) {
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
                                onPlayClick = {
                                    viewModel.focusShort(video.id)
                                    onOpenShorts()
                                },
                                onCreatorClick = {},
                                onLikeClick = { viewModel.toggleLike(video) },
                                onCommentClick = { onOpenVideo(video.id) },
                                onShareClick = { shareVideoIntent(context, video, platformSettings) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                creatorVerified = creator?.verified ?: video.creatorVerified,
                                creatorAffiliatedBrand = creator?.affiliatedBrand.orEmpty(),
                                creatorFounder = creator?.founderAccount == true,
                                creatorJoinedAt = creator?.createdAt,
                                creatorAvatarUrl = creator?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.creatorAvatarUrl
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
                                onShareClick = { shareVideoIntent(context, video, platformSettings) },
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
                                val parsedLinks = buildList {
                                    creator?.channelLinks.orEmpty().forEach { entry ->
                                        val parts = entry.split("|", limit = 2)
                                        val label = parts.getOrNull(0)?.trim().orEmpty()
                                        val url = parts.getOrNull(1)?.trim().orEmpty()
                                        if (url.startsWith("https://")) {
                                            add(label.ifBlank { "Link" } to url)
                                        }
                                    }
                                    val legacy = creator?.socialLink?.trim().orEmpty()
                                    if (legacy.isNotBlank()) {
                                        val normalized = if (legacy.startsWith("https://", ignoreCase = true)) {
                                            legacy
                                        } else if (legacy.startsWith("http://", ignoreCase = true)) {
                                            "https://" + legacy.removePrefix("http://")
                                        } else {
                                            "https://$legacy"
                                        }
                                        if (none { it.second.equals(normalized, ignoreCase = true) }) {
                                            add("Website / Social" to normalized)
                                        }
                                    }
                                }
                                if (parsedLinks.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    Text(
                                        text = "Links",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    parsedLinks.forEach { (label, url) ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { openExternalHttpsUrl(context, url) }
                                                .testTag("channel_about_link_${label.lowercase().replace(" ", "_")}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = url.removePrefix("https://"),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Open link",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    "Public Views: ${formatCount(creator?.totalViews ?: 0L)} • Total Watch Time: ${formatDuration(creator?.totalWatchTimeSec ?: 0L)}",
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

    if (showEditDialog && creator != null) {
        EditProfileDialog(
            creator = creator,
            preferredLanguage = user?.preferredLanguage ?: "Hindi",
            interests = user?.interests.orEmpty(),
            onDismiss = { showEditDialog = false },
            onSave = { name, username, bio, social, channelLinks, lang, newInterests, avatarUri, coverUri, removeAvatar, removeCover ->
                viewModel.updateProfile(
                    displayName = name,
                    username = username,
                    bio = bio,
                    socialLink = social,
                    channelLinks = channelLinks,
                    preferredLanguage = lang,
                    interests = newInterests,
                    avatarUri = avatarUri,
                    coverUri = coverUri,
                    removeAvatar = removeAvatar,
                    removeCover = removeCover,
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
    onSave: (String, String, String, String, List<String>, String, List<String>, Uri?, Uri?, Boolean, Boolean) -> Unit
) {
    var displayName by remember { mutableStateOf(creator.displayName) }
    var username by remember { mutableStateOf(creator.username) }
    var bio by remember { mutableStateOf(creator.bio) }
    var socialLink by remember { mutableStateOf(creator.socialLink) }
    var channelLinks by remember { mutableStateOf(creator.channelLinks) }
    var newLinkLabel by remember { mutableStateOf("") }
    var newLinkUrl by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(preferredLanguage) }
    var selectedInterests by remember { mutableStateOf(interests.toSet()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var removeAvatar by remember { mutableStateOf(false) }
    var removeCover by remember { mutableStateOf(false) }
    var pendingCropAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCropCoverUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingCropAvatarUri = uri
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingCropCoverUri = uri
    }

    pendingCropAvatarUri?.let { rawUri ->
        ImageCropRotateDialog(
            sourceUri = rawUri,
            isCircularAvatar = true,
            onDismiss = { pendingCropAvatarUri = null },
            onCropped = { croppedUri ->
                avatarUri = croppedUri
                removeAvatar = false
                pendingCropAvatarUri = null
            }
        )
    }

    pendingCropCoverUri?.let { rawUri ->
        ImageCropRotateDialog(
            sourceUri = rawUri,
            isCircularAvatar = false,
            onDismiss = { pendingCropCoverUri = null },
            onCropped = { croppedUri ->
                coverUri = croppedUri
                removeCover = false
                pendingCropCoverUri = null
            }
        )
    }

    val hasExistingAvatar = !removeAvatar && (avatarUri != null || isMediaUriAvailable(creator.avatarUrl))
    val hasExistingCover = !removeCover && (coverUri != null || isMediaUriAvailable(creator.coverUrl))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Creator Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLocalFileMissing(creator.avatarUrl) && avatarUri == null && !removeAvatar) {
                    Text(
                        text = "Previous profile photo is unavailable. Tap Change Avatar to choose a new one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlareOrange
                    )
                }

                // Avatar Row: Preview + Crop/Rotate + Remove
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CreatorAvatar(
                        avatarUrl = when {
                            removeAvatar -> ""
                            avatarUri != null -> avatarUri.toString()
                            else -> creator.avatarUrl
                        },
                        name = displayName,
                        size = 52.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (avatarUri != null) "Avatar Cropped ✓" else "Change Avatar",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (hasExistingAvatar) {
                                TextButton(
                                    onClick = {
                                        avatarUri = null
                                        removeAvatar = true
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove profile photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (removeAvatar) {
                            Text(
                                text = "Profile photo will be removed when saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Cover Row: Crop/Rotate + Remove
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    if (coverUri != null && !removeCover) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = "Cropped cover preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (coverUri != null) "Cover Cropped ✓" else "Change Cover",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasExistingCover) {
                            TextButton(
                                onClick = {
                                    coverUri = null
                                    removeCover = true
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove cover photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (removeCover) {
                        Text(
                            text = "Cover photo will be removed when saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(60) }, label = { Text("Display Name") })
                OutlinedTextField(value = username, onValueChange = { username = it.take(30) }, label = { Text("Username") })
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(300) }, label = { Text("Bio") }, maxLines = 3)
                OutlinedTextField(value = socialLink, onValueChange = { socialLink = it.take(200) }, label = { Text("Primary Social Link (Optional)") })

                // Channel About Links (up to 5)
                Text("Channel About Links (${channelLinks.size}/5)", style = MaterialTheme.typography.labelLarge)
                channelLinks.forEachIndexed { idx, raw ->
                    val parts = raw.split("|", limit = 2)
                    val label = parts.getOrNull(0).orEmpty()
                    val url = parts.getOrNull(1).orEmpty()
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { channelLinks = channelLinks.filterIndexed { i, _ -> i != idx } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Remove link", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                if (channelLinks.size < 5) {
                    OutlinedTextField(
                        value = newLinkLabel,
                        onValueChange = { newLinkLabel = it.take(36) },
                        label = { Text("Link Title (e.g. Telegram, Instagram)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLinkUrl,
                        onValueChange = { newLinkUrl = it.take(200) },
                        label = { Text("Link URL (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val cleanUrl = newLinkUrl.trim()
                            if (cleanUrl.isNotBlank()) {
                                val normalized = if (cleanUrl.startsWith("https://", ignoreCase = true)) {
                                    cleanUrl
                                } else {
                                    "https://${cleanUrl.removePrefix("http://")}"
                                }
                                val label = newLinkLabel.trim().ifBlank { "Link ${channelLinks.size + 1}" }
                                channelLinks = (channelLinks + "$label|$normalized").take(5)
                                newLinkLabel = ""
                                newLinkUrl = ""
                            }
                        },
                        enabled = newLinkUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Channel Link")
                    }
                }

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
                    onSave(
                        displayName,
                        username,
                        bio,
                        socialLink,
                        channelLinks,
                        selectedLang,
                        selectedInterests.toList(),
                        avatarUri,
                        coverUri,
                        removeAvatar,
                        removeCover
                    )
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
    onOpenVideo: (String) -> Unit,
    initialTab: Int = 0
) {
    val ownCreatorState by viewModel.ownCreatorState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val monetizationRequest by viewModel.monetizationRequest.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val creator = (ownCreatorState as? UiState.Success)?.data
    val videos = (myVideosState as? UiState.Success)?.data.orEmpty()

    val totalViews = maxOf(creator?.totalViews ?: 0L, videos.sumOf { it.viewsCount })
    val totalEngagedViews = maxOf(creator?.totalEngagedViews ?: 0L, videos.sumOf { it.engagedViewsCount })
    val totalLikes = videos.sumOf { it.likesCount }
    val totalComments = videos.sumOf { it.commentsCount }
    val totalWatchSec = maxOf(creator?.totalWatchTimeSec ?: 0L, videos.sumOf { it.watchTimeTotalSec })
    val watchHoursExact = totalWatchSec / 3600.0
    val watchHoursWhole = totalWatchSec / 3600L
    val shortsEngagedViews = videos.filter { it.shortForm || it.category == "Shorts" }.sumOf { it.engagedViewsCount }
    val shortsPublicViews = videos.filter { it.shortForm || it.category == "Shorts" }.sumOf { it.viewsCount }
    // Use real engaged Shorts views for monetization (with fallback to legacy views if engaged not yet recorded)
    val shortsEngagementViews = if (shortsEngagedViews > 0L) shortsEngagedViews else shortsPublicViews
    val subscribersCount = creator?.followersCount ?: 0L

    val engagementRate = if (totalViews > 0L) {
        String.format(Locale.US, "%.1f%%", ((totalLikes + totalComments) * 100.0) / totalViews)
    } else "0.0%"
    val qualityWatchRatio = if (totalViews > 0L) {
        String.format(Locale.US, "%.0f%%", (totalEngagedViews * 100.0 / totalViews).coerceAtMost(100.0))
    } else "0%"

    val topVideos = remember(videos) { videos.sortedByDescending { it.viewsCount + it.likesCount * 2L } }
    var editingVideo by remember { mutableStateOf<Video?>(null) }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showApplyDialog by remember { mutableStateOf(false) }

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

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Overview & Content") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Monetization") }
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshFeed,
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isOnline) {
                        item {
                            NetworkStatusBanner(
                                isOnline = isOnline,
                                onRetry = viewModel::refreshFeed
                            )
                        }
                    }
                    item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("3-Tier Views Analytics", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "1) Public Views (shown to everyone) • 2) Engaged Views (valid for monetization: 3s+ Shorts / 10s+ Videos) • 3) Creator Quality Insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Public Views", formatCount(totalViews), Modifier.weight(1f))
                            MetricCard("Engaged Views", formatCount(totalEngagedViews), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Quality Ratio", qualityWatchRatio, Modifier.weight(1f))
                            MetricCard("Subscribers", formatCount(subscribersCount), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Watch Time", formatDuration(totalWatchSec), Modifier.weight(1f))
                            MetricCard("Engagement Rate", engagementRate, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Shorts Engaged", formatCount(shortsEngagementViews), Modifier.weight(1f))
                            MetricCard("Videos", videos.size.toString(), Modifier.weight(1f))
                        }
                    }
                }

                // Quick Jump to Partner Monetization Program
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FlareOrange.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonetizationOn, null, tint = FlareOrange)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Earn with JanzFlare Partner", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Track 500 Subscribers, 1,000 Watch Hours, or 100K Shorts Views & Apply",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { selectedTab = 1 }) {
                                Text("Open")
                            }
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
                                        "${video.visibility.uppercase()} • ${formatCount(video.viewsCount)} public • ${formatCount(video.engagedViewsCount)} engaged • ${formatCount(video.likesCount)} likes",
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
        } else {
            MonetizationSectionContent(
                subscribersCount = subscribersCount,
                totalWatchSec = totalWatchSec,
                watchHoursExact = watchHoursExact,
                shortsEngagementViews = shortsEngagementViews,
                monetizationRequest = monetizationRequest,
                onApplyClick = { showApplyDialog = true }
            )
        }
        }
    }

    if (showApplyDialog) {
        var payoutMethod by remember { mutableStateOf("UPI") }
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Apply for JanzFlare Monetization") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Your channel meets the Partner Program criteria. Choose your preferred payout method for review:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    listOf("UPI", "Bank Transfer").forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { payoutMethod = method }
                        ) {
                            RadioButton(
                                selected = payoutMethod == method,
                                onClick = { payoutMethod = method }
                            )
                            Text(method, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitMonetizationApplication(
                            watchTimeHours = watchHoursWhole,
                            shortsEngagementViews = shortsEngagementViews,
                            payoutMethod = payoutMethod
                        )
                        showApplyDialog = false
                    }
                ) {
                    Text("Submit Application")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) { Text("Cancel") }
            }
        )
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
private fun MonetizationSectionContent(
    subscribersCount: Long,
    totalWatchSec: Long,
    watchHoursExact: Double,
    shortsEngagementViews: Long,
    monetizationRequest: MonetizationRequest?,
    onApplyClick: () -> Unit
) {
    val subsMet = subscribersCount >= 500L
    val watchMet = watchHoursExact >= 1000.0
    val shortsMet = shortsEngagementViews >= 100_000L
    val isEligible = subsMet && (watchMet || shortsMet)

    val subsProgress = (subscribersCount / 500f).coerceIn(0f, 1f)
    val watchProgress = (watchHoursExact / 1000.0).toFloat().coerceIn(0f, 1f)
    val shortsProgress = (shortsEngagementViews / 100_000f).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Partner Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(FlareAmber.copy(alpha = 0.7f), FlareOrange, ElectricViolet))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JanzFlareMark(size = 32.dp, animated = false)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("JanzFlare Partner Program", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Earn from ads, Shorts rewards & fan support",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val statusLabel = when (monetizationRequest?.status) {
                        "approved" -> "PARTNER APPROVED"
                        "pending" -> "UNDER REVIEW"
                        "rejected" -> "NEEDS ATTENTION"
                        else -> if (isEligible) "ELIGIBLE TO APPLY" else "IN PROGRESS"
                    }
                    val statusColor = when (monetizationRequest?.status) {
                        "approved" -> Color(0xFF22C55E)
                        "pending" -> FlareAmber
                        "rejected" -> MaterialTheme.colorScheme.error
                        else -> if (isEligible) Color(0xFF22C55E) else FlareOrange
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Eligibility Criteria Progress Bars
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Eligibility Requirements", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Reach 500 Subscribers plus either 1,000 Watch Time Hours or 100K Shorts Engagement Views to unlock Apply.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CriteriaProgressBlock(
                        title = "500 Subscribers",
                        subtitle = "Required",
                        currentText = "${formatCount(subscribersCount)} / 500",
                        progress = subsProgress,
                        met = subsMet
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        "Complete ONE of the following:",
                        style = MaterialTheme.typography.labelLarge,
                        color = FlareOrange
                    )

                    CriteriaProgressBlock(
                        title = "1,000 Watch Time Hours",
                        subtitle = "Public standard videos",
                        currentText = "${formatWatchHours(totalWatchSec)} / 1,000 hrs",
                        progress = watchProgress,
                        met = watchMet
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    CriteriaProgressBlock(
                        title = "100K Shorts Engagement Views",
                        subtitle = "Valid public Shorts views",
                        currentText = "${formatCount(shortsEngagementViews)} / 100K",
                        progress = shortsProgress,
                        met = shortsMet
                    )
                }
            }
        }

        // Apply Action Card (Always visible below criteria)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Monetization Application", style = MaterialTheme.typography.titleMedium)

                    when (monetizationRequest?.status) {
                        "pending" -> {
                            Text(
                                "Your application (Payout: ${monetizationRequest.payoutMethod}) has been submitted and is currently under review by the JanzFlare team.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlareAmber
                            )
                        }
                        "approved" -> {
                            Text(
                                "Congratulations! Your channel is an approved JanzFlare Partner (Payout: ${monetizationRequest.payoutMethod}).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF22C55E)
                            )
                        }
                        else -> {
                            if (monetizationRequest?.status == "rejected") {
                                Text(
                                    "Your previous application was declined. Once eligible, you can re-apply below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = onApplyClick,
                                enabled = isEligible,
                                colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("monetize_apply_button")
                            ) {
                                Icon(
                                    imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isEligible) "Apply for Monetization" else "Apply (Locked Until Eligible)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (!isEligible) {
                                Text(
                                    "Complete 500 Subscribers + (1,000 Watch Hours or 100K Shorts Engagement Views) to activate the Apply button.",
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

@Composable
private fun CriteriaProgressBlock(
    title: String,
    subtitle: String,
    currentText: String,
    progress: Float,
    met: Boolean
) {
    val accent = if (met) Color(0xFF22C55E) else FlareOrange
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = currentText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
                if (met) {
                    Icon(Icons.Default.CheckCircle, "Requirement met", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            color = accent,
            trackColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

private fun formatWatchHours(totalWatchSec: Long): String {
    val hours = totalWatchSec / 3600.0
    return if (hours >= 10.0) {
        String.format(Locale.US, "%.0f", hours)
    } else {
        String.format(Locale.US, "%.1f", hours)
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
