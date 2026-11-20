package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Video
import com.example.domain.RecommendationEngine
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.CategoryChipRow
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.CreatorSpotlightCard
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.JanzBrandHeader
import com.example.ui.components.NetworkStatusBanner
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.ShortsStripSkeleton
import com.example.ui.components.VideoCardSkeleton
import com.example.ui.components.VideoThumbnailCard
import com.example.ui.components.formatCount
import com.example.ui.components.isMediaUriAvailable
import com.example.ui.components.shareVideoIntent
import com.example.ui.state.HomeFeedTab
import com.example.ui.state.JanzViewModel
import com.example.ui.state.SearchSortMode
import com.example.ui.state.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JanzViewModel,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenOwnProfile: () -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenShorts: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val homeFeed by viewModel.homeFeedVideos.collectAsStateWithLifecycle()
    val shortsFeed by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val ownCreatorState by viewModel.ownCreatorState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val user = (userState as? UiState.Success)?.data
    val ownCreator = (ownCreatorState as? UiState.Success)?.data
    val unreadNotifs = notifications.count { !it.read }
    val blocked = user?.blockedUserIds?.toSet().orEmpty()
    val followingIds = remember(following) { following.map { it.creatorId }.toSet() }
    val notifyMap = remember(following) { following.associate { it.creatorId to it.notifyOnUpload } }
    val creatorsById = remember(creators) { creators.associateBy { it.creatorId } }
    val popularCreators = remember(creators, blocked) { RecommendationEngine.rankPopularCreators(creators, blocked).take(10) }
    val newCreators = remember(creators, blocked) { RecommendationEngine.rankNewCreators(creators, blocked).take(10) }

    var reportVideoTarget by remember { mutableStateOf<Video?>(null) }
    var playlistVideoId by remember { mutableStateOf<String?>(null) }
    var updateBannerDismissed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { JanzBrandHeader() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            actions = {
                if (isAdmin) {
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier.testTag("home_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Dashboard",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("home_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifs > 0) {
                                Badge { Text(unreadNotifs.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
                IconButton(
                    onClick = onOpenOwnProfile,
                    modifier = Modifier.testTag("home_profile_button")
                ) {
                    CreatorAvatar(
                        avatarUrl = ownCreator?.avatarUrl.orEmpty(),
                        name = ownCreator?.displayName ?: "Me",
                        size = 32.dp
                    )
                }
            }
        )

        // Feed Tabs
        ScrollableTabRow(
            selectedTabIndex = HomeFeedTab.entries.indexOf(selectedTab),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            divider = {}
        ) {
            HomeFeedTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectHomeTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshFeed,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                // Admin-controlled App Update Banner (Telegram / APK / Play Store)
                item {
                    AppUpdateBanner(
                        settings = platformSettings,
                        dismissed = updateBannerDismissed,
                        onDismiss = { updateBannerDismissed = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Optional Admin Notice Banner
                val bannerText = platformSettings?.featuredBannerText.orEmpty()
                val maintText = platformSettings?.maintenanceNotice.orEmpty()
                if (bannerText.isNotBlank() || maintText.isNotBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = maintText.ifBlank { bannerText },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Categories Filter
                item {
                    CategoryChipRow(
                        categories = activeCategories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = viewModel::selectCategoryFilter,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Shorts Spotlight Strip
                if (isRefreshing || (publicVideosState is UiState.Loading && shortsFeed.isEmpty())) {
                    item { ShortsStripSkeleton() }
                } else if (shortsFeed.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Shorts", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = {
                                    viewModel.clearFocusedShort()
                                    onOpenShorts()
                                }) {
                                    Text("Watch All")
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(shortsFeed.take(10), key = { it.id }) { shortVideo ->
                                    ShortPreviewCard(
                                        video = shortVideo,
                                        onClick = {
                                            viewModel.focusShort(shortVideo.id)
                                            onOpenShorts()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Video Feed State
                if (isRefreshing || publicVideosState is UiState.Loading) {
                    items(3) {
                        VideoCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                } else when (publicVideosState) {
                    is UiState.Loading -> {
                        items(3) {
                            VideoCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    is UiState.Error -> {
                        val err = (publicVideosState as UiState.Error).message
                        item {
                            EmptyStudioCard(
                                title = "Could not refresh feed",
                                subtitle = err,
                                actionLabel = "Retry",
                                onActionClick = viewModel::refreshFeed,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (homeFeed.isEmpty()) {
                            item {
                                EmptyStudioCard(
                                    title = if (selectedTab == HomeFeedTab.FOLLOWING) {
                                        "No videos from subscribed creators yet"
                                    } else {
                                        "Be the First Creator to Spark JanzFlare"
                                    },
                                    subtitle = "Upload a video or Short from your phone gallery to publish to the community feed.",
                                    actionLabel = "Upload Video",
                                    onActionClick = onOpenUpload,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                        items(homeFeed, key = { it.id }) { video ->
                            val videoCreator = creatorsById[video.creatorId]
                            VideoThumbnailCard(
                                video = video,
                                isLiked = likedIds.contains(video.id),
                                isSaved = user?.savedVideoIds?.contains(video.id) == true,
                                onPlayClick = { onOpenVideo(video.id) },
                                onCreatorClick = { onOpenCreatorProfile(video.creatorId) },
                                onLikeClick = { viewModel.toggleLike(video) },
                                onCommentClick = { onOpenVideo(video.id) },
                                onShareClick = { shareVideoIntent(context, video, platformSettings) },
                                onSaveClick = { viewModel.toggleSavedVideo(video) },
                                onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                                onAddToPlaylistClick = { playlistVideoId = video.id },
                                onReportClick = { reportVideoTarget = video },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                creatorVerified = videoCreator?.verified ?: video.creatorVerified,
                                creatorAffiliatedBrand = videoCreator?.affiliatedBrand.orEmpty(),
                                creatorFounder = videoCreator?.founderAccount == true,
                                creatorJoinedAt = videoCreator?.createdAt,
                                creatorAvatarUrl = videoCreator?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.creatorAvatarUrl
                            )
                        }
                    }
                }
            }

            // New & Popular Creators Discovery Carousels
            if (newCreators.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "New Creators to Discover",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(newCreators, key = { "new_${it.creatorId}" }) { creator ->
                                val bellEnabled = notifyMap[creator.creatorId] ?: true
                                CreatorSpotlightCard(
                                    creator = creator,
                                    isFollowing = creator.creatorId in followingIds,
                                    isSelf = creator.creatorId == viewModel.currentUserId,
                                    badgeLabel = "Rising Creator",
                                    onClick = { onOpenCreatorProfile(creator.creatorId) },
                                    onFollowClick = { viewModel.toggleFollow(creator) },
                                    notifyOnUpload = bellEnabled,
                                    onToggleBell = { viewModel.toggleSubscriptionBell(creator, bellEnabled) }
                                )
                            }
                        }
                    }
                }
            }

            if (popularCreators.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Popular Creators",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(popularCreators, key = { "pop_${it.creatorId}" }) { creator ->
                                val bellEnabled = notifyMap[creator.creatorId] ?: true
                                CreatorSpotlightCard(
                                    creator = creator,
                                    isFollowing = creator.creatorId in followingIds,
                                    isSelf = creator.creatorId == viewModel.currentUserId,
                                    badgeLabel = "Popular",
                                    onClick = { onOpenCreatorProfile(creator.creatorId) },
                                    onFollowClick = { viewModel.toggleFollow(creator) },
                                    notifyOnUpload = bellEnabled,
                                    onToggleBell = { viewModel.toggleSubscriptionBell(creator, bellEnabled) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
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
}

@Composable
private fun ShortPreviewCard(
    video: Video,
    onClick: () -> Unit
) {
    val thumbAvailable = remember(video.thumbnailUri) { isMediaUriAvailable(video.thumbnailUri) }
    var thumbError by remember(video.thumbnailUri) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(126.dp)
            .height(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbAvailable && !thumbError) {
                AsyncImage(
                    model = video.thumbnailUri,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    onError = { thumbError = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_studio),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatCount(video.viewsCount)} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreSearchScreen(
    viewModel: JanzViewModel,
    onOpenVideo: (String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenShorts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.searchSortMode.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val following by viewModel.followingList.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchScope by remember { mutableStateOf("All") }
    var reportVideoTarget by remember { mutableStateOf<Video?>(null) }
    var playlistVideoId by remember { mutableStateOf<String?>(null) }

    val user = (userState as? UiState.Success)?.data
    val blocked = user?.blockedUserIds?.toSet().orEmpty()
    val allVideos = (publicVideosState as? UiState.Success)?.data?.filter { it.creatorId !in blocked }.orEmpty()
    val followingIds = remember(following) { following.map { it.creatorId }.toSet() }
    val notifyMap = remember(following) { following.associate { it.creatorId to it.notifyOnUpload } }
    val creatorsById = remember(creators) { creators.associateBy { it.creatorId } }

    val trendingTags = remember(allVideos) {
        allVideos.flatMap { it.tags }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { "#${it.key}" }
            .ifEmpty { listOf("#India", "#Music", "#Rajasthan", "#Tech", "#Comedy", "#Vlogs") }
    }

    val suggestions = remember(query, allVideos, creators) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            (allVideos.map { it.title } +
                creators.map { it.displayName } +
                allVideos.flatMap { it.tags.map { tag -> "#$tag" } })
                .distinct()
                .filter { it.lowercase().contains(q) && !it.equals(query, ignoreCase = true) }
                .take(5)
        }
    }

    val filteredVideos = remember(allVideos, query, selectedCategory, searchScope, sortMode) {
        val q = query.trim().removePrefix("#").lowercase()
        allVideos.filter { v ->
            val catOk = selectedCategory == null || v.category == selectedCategory
            val scopeOk = when (searchScope) {
                "Shorts" -> v.shortForm || v.category == "Shorts"
                "Videos" -> !v.shortForm
                else -> true
            }
            val queryOk = q.isBlank() ||
                v.title.lowercase().contains(q) ||
                v.description.lowercase().contains(q) ||
                v.creatorName.lowercase().contains(q) ||
                v.category.lowercase().contains(q) ||
                v.tags.any { it.lowercase().contains(q) }
            catOk && scopeOk && queryOk
        }.let { list ->
            when (sortMode) {
                SearchSortMode.LATEST -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                SearchSortMode.POPULAR -> list.sortedByDescending { it.viewsCount + it.likesCount * 3L }
                SearchSortMode.RELEVANCE -> list.sortedByDescending { it.likesCount * 2L + it.viewsCount }
            }
        }
    }

    val filteredCreators = remember(creators, query, blocked) {
        val q = query.trim().lowercase()
        creators.filter { c ->
            c.creatorId !in blocked &&
                (q.isBlank() || c.displayName.lowercase().contains(q) || c.username.lowercase().contains(q))
        }.sortedByDescending { it.founderAccount }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Explore & Search", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setSearchQuery,
            placeholder = { Text("Search videos, creators, #hashtags, categories...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("explore_search_input")
        )

        Spacer(Modifier.height(10.dp))

        // Suggestions while typing
        if (suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    AssistChip(
                        onClick = {
                            viewModel.setSearchQuery(suggestion)
                            viewModel.addRecentSearch(suggestion)
                        },
                        label = { Text(suggestion) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Scope & Sort Filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Videos", "Shorts", "Creators")) { scope ->
                FilterChip(
                    selected = searchScope == scope,
                    onClick = { searchScope = scope },
                    label = { Text(scope) }
                )
            }
            items(SearchSortMode.entries) { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { viewModel.setSearchSortMode(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        CategoryChipRow(
            categories = activeCategories,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it }
        )
        Spacer(Modifier.height(10.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshFeed,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
            if (query.isBlank()) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = viewModel::clearRecentSearches) {
                                    Text("Clear History")
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recentSearches.forEach { term ->
                                    AssistChip(
                                        onClick = { viewModel.setSearchQuery(term) },
                                        leadingIcon = { Icon(Icons.Default.History, null, Modifier.size(16.dp)) },
                                        label = { Text(term) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Trending Topics & Hashtags", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            trendingTags.forEach { tag ->
                                AssistChip(
                                    onClick = {
                                        viewModel.setSearchQuery(tag)
                                        viewModel.addRecentSearch(tag)
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }

            if (searchScope == "All" || searchScope == "Creators") {
                if (filteredCreators.isNotEmpty()) {
                    item {
                        Column {
                            Text("Creators", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filteredCreators, key = { it.creatorId }) { creator ->
                                    val bellEnabled = notifyMap[creator.creatorId] ?: true
                                    CreatorSpotlightCard(
                                        creator = creator,
                                        isFollowing = creator.creatorId in followingIds,
                                        isSelf = creator.creatorId == viewModel.currentUserId,
                                        badgeLabel = "@${creator.username}",
                                        onClick = {
                                            if (query.isNotBlank()) viewModel.addRecentSearch(query)
                                            onOpenCreatorProfile(creator.creatorId)
                                        },
                                        onFollowClick = { viewModel.toggleFollow(creator) },
                                        notifyOnUpload = bellEnabled,
                                        onToggleBell = { viewModel.toggleSubscriptionBell(creator, bellEnabled) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (searchScope != "Creators") {
                if (isRefreshing || publicVideosState is UiState.Loading) {
                    items(3) {
                        VideoCardSkeleton()
                    }
                } else if (filteredVideos.isEmpty()) {
                    item {
                        EmptyStudioCard(
                            title = if (!isOnline) "Network unavailable" else "No matching videos found",
                            subtitle = if (!isOnline) "Check your internet connection and pull down to retry." else "Try another keyword, category, or language filter.",
                            actionLabel = if (!isOnline) "Retry" else null,
                            onActionClick = if (!isOnline) viewModel::refreshFeed else null
                        )
                    }
                } else {
                    items(filteredVideos, key = { it.id }) { video ->
                        val videoCreator = creatorsById[video.creatorId]
                        VideoThumbnailCard(
                            video = video,
                            isLiked = likedIds.contains(video.id),
                            isSaved = user?.savedVideoIds?.contains(video.id) == true,
                            onPlayClick = {
                                if (query.isNotBlank()) viewModel.addRecentSearch(query)
                                if (video.shortForm) {
                                    viewModel.focusShort(video.id)
                                    onOpenShorts()
                                } else {
                                    onOpenVideo(video.id)
                                }
                            },
                            onCreatorClick = { onOpenCreatorProfile(video.creatorId) },
                            onLikeClick = { viewModel.toggleLike(video) },
                            onCommentClick = { onOpenVideo(video.id) },
                            onShareClick = { shareVideoIntent(context, video, platformSettings) },
                            onSaveClick = { viewModel.toggleSavedVideo(video) },
                            onWatchLaterClick = { viewModel.toggleWatchLater(video.id) },
                            onAddToPlaylistClick = { playlistVideoId = video.id },
                            onReportClick = { reportVideoTarget = video },
                            creatorVerified = videoCreator?.verified ?: video.creatorVerified,
                            creatorAffiliatedBrand = videoCreator?.affiliatedBrand.orEmpty(),
                            creatorFounder = videoCreator?.founderAccount == true,
                            creatorJoinedAt = videoCreator?.createdAt,
                            creatorAvatarUrl = videoCreator?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.creatorAvatarUrl
                        )
                    }
                }
            }
        }
        }
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
}
