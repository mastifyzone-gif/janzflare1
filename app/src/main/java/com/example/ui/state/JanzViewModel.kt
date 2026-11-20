package com.example.ui.state

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.Comment
import com.example.data.model.CreatorProfile
import com.example.data.model.FollowRecord
import com.example.data.model.JanzCatalog
import com.example.data.model.ModerationReport
import com.example.data.model.MonetizationRequest
import com.example.data.model.PlatformSettings
import com.example.data.model.Playlist
import com.example.data.model.UserAccount
import com.example.data.model.Video
import com.example.data.model.WatchHistoryRecord
import com.example.data.repository.JanzRepository
import com.example.domain.RecommendationEngine
import com.example.notifications.JanzNotificationHelper
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HomeFeedTab(val label: String) {
    FOR_YOU("For You"),
    FOLLOWING("Subscriptions"),
    TRENDING("Trending"),
    LATEST("Latest"),
    FEATURED("Featured")
}

enum class SearchSortMode(val label: String) {
    RELEVANCE("Best Match"),
    LATEST("Latest"),
    POPULAR("Popular")
}

data class UploadProgressState(
    val uploading: Boolean = false,
    val progress: Float = 0f,
    val mbText: String = "",
    val statusText: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class JanzViewModel(
    private val appContext: Context,
    private val repository: JanzRepository,
    val currentUserId: String,
    private val currentFirebaseUser: FirebaseUser
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeFeedTab.FOR_YOU)
    val selectedHomeTab: StateFlow<HomeFeedTab> = _selectedHomeTab.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSortMode = MutableStateFlow(SearchSortMode.RELEVANCE)
    val searchSortMode: StateFlow<SearchSortMode> = _searchSortMode.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _uploadState = MutableStateFlow(UploadProgressState())
    val uploadState: StateFlow<UploadProgressState> = _uploadState.asStateFlow()

    private val _activeWatchVideoId = MutableStateFlow<String?>(null)

    private val _targetCreatorId = MutableStateFlow<String?>(null)

    private val _focusedShortId = MutableStateFlow<String?>(null)
    val focusedShortId: StateFlow<String?> = _focusedShortId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Lightweight client-side anti-spam rate limiters
    private var lastCommentAtMs: Long = 0L
    private var lastReportAtMs: Long = 0L
    private var lastEngagementAtMs: Long = 0L

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkCurrentConnectivity()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun friendlyPublicError(error: Throwable, fallback: String): String {
        val raw = error.localizedMessage.orEmpty()
        val isInternalTechError = raw.isBlank() ||
            error is FirebaseFirestoreException ||
            raw.contains("firestore", ignoreCase = true) ||
            raw.contains("firebase", ignoreCase = true) ||
            raw.contains("permission_denied", ignoreCase = true) ||
            raw.contains("database", ignoreCase = true) ||
            raw.contains("grpc", ignoreCase = true) ||
            raw.contains("unavailable", ignoreCase = true)
        return if (isInternalTechError) fallback else raw
    }

    private fun <T> Flow<T>.toUiState(defaultError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("JanzVM", defaultError, root)
                emit(UiState.Error(friendlyPublicError(root, defaultError)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userAccountState: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId).toUiState("Failed to load user account")

    val ownCreatorState: StateFlow<UiState<CreatorProfile?>> =
        repository.observeCreatorProfile(currentUserId).toUiState("Failed to load creator profile")

    val targetCreatorState: StateFlow<UiState<CreatorProfile?>> = _targetCreatorId
        .flatMapLatest { cid ->
            if (cid.isNullOrBlank()) flowOf(UiState.Success(null))
            else repository.observeCreatorProfile(cid).toUiState("Failed to load creator profile")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allCreators: StateFlow<List<CreatorProfile>> =
        repository.observeAllCreators()
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publicVideosState: StateFlow<UiState<List<Video>>> =
        repository.observePublicVideos().toUiState("Failed to load video feed")

    val myVideosState: StateFlow<UiState<List<Video>>> =
        repository.observeMyVideos(currentUserId).toUiState("Failed to load your videos")

    val likedVideoIds: StateFlow<Set<String>> =
        repository.observeLikedVideoIds(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followingList: StateFlow<List<FollowRecord>> =
        repository.observeFollowing(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryRecord>> =
        repository.observeWatchHistory(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repository.observePlaylists(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        repository.observeNotifications(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val platformSettings: StateFlow<PlatformSettings?> =
        repository.observePlatformSettings()
            .catch { if (it is CancellationException) throw it else emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeComments: StateFlow<List<Comment>> = _activeWatchVideoId
        .flatMapLatest { videoId ->
            if (videoId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeComments(videoId)
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reports: StateFlow<List<ModerationReport>> = _isAdmin
        .flatMapLatest { admin ->
            repository.observeReports(currentUserId, admin)
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminCreators: StateFlow<List<CreatorProfile>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllCreatorsForAdmin()
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminVideos: StateFlow<List<Video>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllVideosForAdmin()
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val monetizationRequest: StateFlow<MonetizationRequest?> =
        repository.observeMonetizationRequest(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val adminMonetizationRequests: StateFlow<List<MonetizationRequest>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllMonetizationRequestsForAdmin()
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCategories: StateFlow<List<String>> = platformSettings
        .map { settings ->
            val disabled = settings?.disabledCategories?.toSet().orEmpty()
            JanzCatalog.allCategories.filterNot { it in disabled }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), JanzCatalog.allCategories)

    val homeFeedVideos: StateFlow<List<Video>> = combine(
        publicVideosState,
        allCreators,
        userAccountState,
        watchHistory,
        followingList,
        _selectedHomeTab,
        _selectedCategoryFilter
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val videosState = values[0] as UiState<List<Video>>
        @Suppress("UNCHECKED_CAST")
        val creators = values[1] as List<CreatorProfile>
        @Suppress("UNCHECKED_CAST")
        val userState = values[2] as UiState<UserAccount?>
        @Suppress("UNCHECKED_CAST")
        val history = values[3] as List<WatchHistoryRecord>
        @Suppress("UNCHECKED_CAST")
        val following = values[4] as List<FollowRecord>
        val tab = values[5] as HomeFeedTab
        val catFilter = values[6] as String?

        val rawVideos = (videosState as? UiState.Success)?.data ?: emptyList()
        val user = (userState as? UiState.Success)?.data
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val creatorsById = creators.associateBy { it.creatorId }
        val filteredByCategory = rawVideos.filter { v ->
            v.creatorId !in blocked && (catFilter == null || v.category == catFilter)
        }

        val catWatchMap = history.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        val langWatchMap = history.groupBy { it.language }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        val followingIds = following.map { it.creatorId }.toSet()

        when (tab) {
            HomeFeedTab.FOR_YOU -> RecommendationEngine.rankForYou(
                videos = filteredByCategory,
                creatorsById = creatorsById,
                user = user,
                followingIds = followingIds,
                watchHistory = history,
                watchHistoryCategories = catWatchMap,
                watchHistoryLanguages = langWatchMap
            )
            HomeFeedTab.FOLLOWING -> filteredByCategory
                .filter { it.creatorId in followingIds }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            HomeFeedTab.TRENDING -> RecommendationEngine.rankTrending(filteredByCategory, blocked)
            HomeFeedTab.LATEST -> filteredByCategory.sortedByDescending { it.createdAt?.seconds ?: 0L }
            HomeFeedTab.FEATURED -> filteredByCategory.filter { it.featured }
                .ifEmpty { filteredByCategory.sortedByDescending { it.likesCount } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shortsFeed: StateFlow<List<Video>> = combine(
        publicVideosState,
        allCreators,
        userAccountState,
        followingList,
        watchHistory
    ) { vState, creators, uState, following, history ->
        val raw = (vState as? UiState.Success)?.data ?: emptyList()
        val user = (uState as? UiState.Success)?.data
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val shorts = raw.filter { (it.shortForm || it.category == "Shorts") && it.creatorId !in blocked }
        val creatorsById = creators.associateBy { it.creatorId }
        val followingIds = following.map { it.creatorId }.toSet()
        val catWatchMap = history.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        val langWatchMap = history.groupBy { it.language }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        RecommendationEngine.rankShortsForYou(
            shorts = shorts,
            creatorsById = creatorsById,
            user = user,
            followingIds = followingIds,
            watchHistory = history,
            watchHistoryCategories = catWatchMap,
            watchHistoryLanguages = langWatchMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        runCatching { connectivityManager?.registerDefaultNetworkCallback(networkCallback) }
        viewModelScope.launch {
            repository.ensureUserAndCreatorInitialized(currentFirebaseUser)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to initialize profile" }
            _isAdmin.value = repository.checkIsAdmin(currentFirebaseUser)
        }
        viewModelScope.launch {
            JanzNotificationHelper.ensureChannel(appContext)
            combine(notifications, userAccountState) { notifs, uState ->
                notifs to uState
            }.collect { (notifs, uState) ->
                if (uState !is UiState.Loading) {
                    val userPrefs = (uState as? UiState.Success)?.data
                    JanzNotificationHelper.processIncomingNotifications(
                        context = appContext,
                        userId = currentUserId,
                        notifications = notifs,
                        userPrefs = userPrefs
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
    }

    fun refreshFeed() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _isOnline.value = checkCurrentConnectivity()
            if (!_isOnline.value) {
                delay(450L)
                _isRefreshing.value = false
                _snackbarMessage.value = "Weak or no internet connection. Please check your network."
                return@launch
            }
            val startMs = System.currentTimeMillis()
            val refreshResult = withTimeoutOrNull(10_000L) {
                repository.forceRefreshFromServer(currentUserId)
            }
            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed < 600L) delay(600L - elapsed)
            _isRefreshing.value = false
            if (refreshResult == null || refreshResult.isFailure) {
                _snackbarMessage.value = "Weak internet connection. Please pull down to try again."
            }
        }
    }

    fun selectHomeTab(tab: HomeFeedTab) { _selectedHomeTab.value = tab }
    fun selectCategoryFilter(category: String?) { _selectedCategoryFilter.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchSortMode(mode: SearchSortMode) { _searchSortMode.value = mode }
    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            _recentSearches.value = (listOf(clean) + _recentSearches.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        }
    }
    fun clearRecentSearches() { _recentSearches.value = emptyList() }
    fun setActiveWatchVideo(videoId: String?) { _activeWatchVideoId.value = videoId }
    fun setTargetCreatorId(creatorId: String?) { _targetCreatorId.value = creatorId }
    fun focusShort(videoId: String?) { _focusedShortId.value = videoId }
    fun clearFocusedShort() { _focusedShortId.value = null }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun showMessage(msg: String) { _snackbarMessage.value = msg }

    fun publishVideo(
        title: String,
        description: String,
        pickedVideoUri: Uri?,
        remoteVideoUrl: String,
        pickedThumbUri: Uri?,
        category: String,
        tagsInput: String,
        language: String,
        visibility: String,
        shortForm: Boolean,
        onPublished: (String) -> Unit
    ) {
        val creator = (ownCreatorState.value as? UiState.Success)?.data ?: return
        val tags = tagsInput.split(",", " ").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _uploadState.value = UploadProgressState(
                uploading = true,
                progress = 0.03f,
                mbText = "",
                statusText = "Preparing upload..."
            )
            JanzNotificationHelper.showUploadProgressNotification(
                context = appContext,
                videoTitle = title,
                progressPct = 3,
                indeterminate = true,
                detailText = "Preparing upload..."
            )
            val result = repository.publishVideo(
                creator = creator,
                title = title,
                description = description,
                pickedVideoUri = pickedVideoUri,
                remoteVideoUrl = remoteVideoUrl,
                pickedThumbUri = pickedThumbUri,
                category = category,
                tags = tags,
                language = language,
                visibility = visibility,
                shortForm = shortForm,
                onProgress = { progress, mbText, statusText, indeterminate ->
                    _uploadState.value = UploadProgressState(
                        uploading = true,
                        progress = progress,
                        mbText = mbText,
                        statusText = statusText
                    )
                    val pct = (progress * 100f).toInt().coerceIn(0, 100)
                    JanzNotificationHelper.showUploadProgressNotification(
                        context = appContext,
                        videoTitle = title,
                        progressPct = pct,
                        indeterminate = indeterminate,
                        detailText = statusText
                    )
                }
            )
            _uploadState.value = UploadProgressState()
            result.onSuccess { newId ->
                JanzNotificationHelper.showUploadCompleteNotification(
                    context = appContext,
                    videoId = newId,
                    videoTitle = title,
                    success = true,
                    message = "Video published successfully!"
                )
                _snackbarMessage.value = "100% • Video uploaded & published!"
                onPublished(newId)
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "Failed to publish video"
                JanzNotificationHelper.showUploadCompleteNotification(
                    context = appContext,
                    videoId = null,
                    videoTitle = title,
                    success = false,
                    message = errMsg
                )
                _snackbarMessage.value = errMsg
            }
        }
    }

    fun toggleLike(video: Video) {
        val now = System.currentTimeMillis()
        if (now - lastEngagementAtMs < 350L) return
        lastEngagementAtMs = now
        val creator = (ownCreatorState.value as? UiState.Success)?.data
        val currentlyLiked = likedVideoIds.value.contains(video.id)
        viewModelScope.launch {
            repository.toggleLike(video, currentlyLiked, creator?.displayName ?: "Someone")
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Action failed" }
        }
    }

    fun toggleFollow(creator: CreatorProfile) {
        val now = System.currentTimeMillis()
        if (now - lastEngagementAtMs < 350L) return
        lastEngagementAtMs = now
        val selfCreator = (ownCreatorState.value as? UiState.Success)?.data
        val currentlyFollowing = followingList.value.any { it.creatorId == creator.creatorId }
        viewModelScope.launch {
            repository.toggleFollow(creator, currentlyFollowing, selfCreator?.displayName ?: "Someone")
                .onSuccess {
                    _snackbarMessage.value = if (currentlyFollowing) {
                        "Unsubscribed from ${creator.displayName}"
                    } else {
                        "Subscribed to ${creator.displayName} (Bell ON)"
                    }
                }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Subscription update failed") }
        }
    }

    fun toggleSubscriptionBell(creator: CreatorProfile, currentlyEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSubscriptionBell(creator.creatorId, currentlyEnabled)
                .onSuccess {
                    _snackbarMessage.value = if (!currentlyEnabled) {
                        "Upload notifications ON for ${creator.displayName}"
                    } else {
                        "Upload notifications muted for ${creator.displayName}"
                    }
                }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to update notification bell") }
        }
    }

    fun addComment(video: Video, text: String, parentCommentId: String = "") {
        val now = System.currentTimeMillis()
        if (now - lastCommentAtMs < 2_000L) {
            _snackbarMessage.value = "Please wait a moment before posting another comment."
            return
        }
        lastCommentAtMs = now
        val selfCreator = (ownCreatorState.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.addComment(
                video = video,
                text = text,
                parentCommentId = parentCommentId,
                authorName = selfCreator?.displayName ?: currentFirebaseUser.displayName ?: "User",
                authorAvatarUrl = selfCreator?.avatarUrl.orEmpty()
            ).onFailure { _snackbarMessage.value = friendlyPublicError(it, "Could not post comment") }
        }
    }

    fun togglePinComment(videoId: String, comment: Comment) {
        viewModelScope.launch {
            repository.togglePinComment(videoId, comment)
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Could not update comment") }
        }
    }

    fun deleteComment(video: Video, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(video, commentId)
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Could not delete comment") }
        }
    }

    fun toggleSavedVideo(video: Video) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleSavedVideo(user, video)
                .onSuccess {
                    val savedNow = !user.savedVideoIds.contains(video.id)
                    _snackbarMessage.value = if (savedNow) "Saved to library" else "Removed from saved"
                }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Could not save video") }
        }
    }

    fun toggleWatchLater(videoId: String) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleWatchLater(user, videoId)
                .onSuccess {
                    val added = !user.watchLaterVideoIds.contains(videoId)
                    _snackbarMessage.value = if (added) "Added to Watch Later" else "Removed from Watch Later"
                }
        }
    }

    fun createPlaylist(title: String, description: String, initialVideoId: String? = null) {
        viewModelScope.launch {
            repository.createPlaylist(title, description, initialVideoId)
                .onSuccess { _snackbarMessage.value = "Playlist created" }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to create playlist") }
        }
    }

    fun toggleVideoInPlaylist(playlist: Playlist, videoId: String) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(playlist, videoId)
                .onSuccess { _snackbarMessage.value = "Updated playlist \"${playlist.title}\"" }
        }
    }

    fun recordWatch(
        video: Video,
        deltaWatchSec: Long,
        countNewView: Boolean,
        countEngagedView: Boolean = false
    ) {
        viewModelScope.launch {
            repository.recordVideoViewAndWatchTime(video, deltaWatchSec, countNewView, countEngagedView)
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        socialLink: String,
        channelLinks: List<String> = emptyList(),
        preferredLanguage: String,
        interests: List<String>,
        avatarUri: Uri?,
        coverUri: Uri?,
        removeAvatar: Boolean = false,
        removeCover: Boolean = false,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                displayName = displayName,
                username = username,
                bio = bio,
                socialLink = socialLink,
                channelLinks = channelLinks,
                preferredLanguage = preferredLanguage,
                interests = interests,
                avatarUri = avatarUri,
                coverUri = coverUri,
                removeAvatar = removeAvatar,
                removeCover = removeCover
            )
                .onSuccess {
                    _snackbarMessage.value = "Profile updated"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to update profile") }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateNotificationPrefs(likes: Boolean, comments: Boolean, followers: Boolean, uploads: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(likes, comments, followers, uploads)
                .onSuccess { _snackbarMessage.value = "Notification preferences saved" }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun toggleBlockUser(targetUserId: String) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleBlockUser(user, targetUserId)
                .onSuccess {
                    val blockedNow = !user.blockedUserIds.contains(targetUserId)
                    _snackbarMessage.value = if (blockedNow) "User blocked" else "User unblocked"
                }
        }
    }

    fun submitReport(targetType: String, targetId: String, targetVideoId: String, reason: String, details: String) {
        val now = System.currentTimeMillis()
        if (now - lastReportAtMs < 5_000L) {
            _snackbarMessage.value = "Please wait a few seconds before submitting another report."
            return
        }
        lastReportAtMs = now
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, targetVideoId, reason, details)
                .onSuccess { _snackbarMessage.value = "Report submitted for review" }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to submit report") }
        }
    }

    fun updateOwnVideo(videoId: String, title: String, description: String, category: String, language: String, visibility: String) {
        viewModelScope.launch {
            repository.updateVideoMetadata(videoId, title, description, category, language, visibility)
                .onSuccess { _snackbarMessage.value = "Video updated" }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to update video") }
        }
    }

    fun deleteOwnVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
                .onSuccess { _snackbarMessage.value = "Video deleted" }
                .onFailure { _snackbarMessage.value = friendlyPublicError(it, "Failed to delete video") }
        }
    }

    fun deleteOwnAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteOwnAccountData()
                .onSuccess { onDeleted() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete account" }
        }
    }

    // --- Admin Actions ---
    fun adminSetCreatorVerified(creatorId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.adminSetCreatorVerified(creatorId, verified)
                .onSuccess { _snackbarMessage.value = if (verified) "Creator verified" else "Verification removed" }
        }
    }

    fun adminSetCreatorAffiliation(creatorId: String, affiliatedBrand: String) {
        viewModelScope.launch {
            repository.adminSetCreatorAffiliation(creatorId, affiliatedBrand)
                .onSuccess {
                    _snackbarMessage.value = if (affiliatedBrand.isNotBlank()) {
                        "Affiliated with $affiliatedBrand"
                    } else {
                        "Affiliation badge removed"
                    }
                }
        }
    }

    fun adminSetCreatorStatus(creatorId: String, status: String) {
        viewModelScope.launch {
            repository.adminSetCreatorStatus(creatorId, status)
                .onSuccess { _snackbarMessage.value = "Creator status set to $status" }
        }
    }

    fun adminModerateVideo(videoId: String, moderationStatus: String, featured: Boolean, trendingPinned: Boolean) {
        viewModelScope.launch {
            repository.adminModerateVideo(videoId, moderationStatus, featured, trendingPinned)
                .onSuccess { _snackbarMessage.value = "Video moderation updated" }
        }
    }

    fun adminUpdateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateReportStatus(reportId, status)
                .onSuccess { _snackbarMessage.value = "Report marked $status" }
        }
    }

    fun adminUpdatePlatformSettings(
        settings: PlatformSettings,
        successMessage: String? = "Platform settings saved & live!",
        silentOnFailure: Boolean = false
    ) {
        viewModelScope.launch {
            repository.adminUpdatePlatformSettings(settings)
                .onSuccess {
                    if (!successMessage.isNullOrBlank()) {
                        _snackbarMessage.value = successMessage
                    }
                }
                .onFailure { err ->
                    if (!silentOnFailure) {
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to save settings"
                    }
                }
        }
    }

    fun submitMonetizationApplication(watchTimeHours: Long, shortsEngagementViews: Long, payoutMethod: String) {
        val creator = (ownCreatorState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.submitMonetizationApplication(
                creator = creator,
                watchTimeHours = watchTimeHours,
                shortsEngagementViews = shortsEngagementViews,
                payoutMethod = payoutMethod
            ).onSuccess {
                _snackbarMessage.value = "Monetization application submitted for review!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not submit monetization application"
            }
        }
    }

    fun adminUpdateMonetizationStatus(creatorId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateMonetizationStatus(creatorId, status)
                .onSuccess {
                    _snackbarMessage.value = "Monetization status updated to ${status.replaceFirstChar { it.uppercase() }}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update monetization status"
                }
        }
    }
}
